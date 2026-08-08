# Phase 5 — Service-to-Service Authentication

**Status:** Implemented
**Date:** 2026-08-08

---

## The gap Phase 5 closes

By the end of Phase 4, Auth, User, and Chat were three independently deployable services calling each other over plain HTTP:

- `Auth → User` (`UserProfileClient.createProfile`, part of the register saga)
- `Chat → User` (`UserServiceClient.batchGetByIds`, `UserServiceClient.ensureFriendship`)

Both calls hit endpoints under User's `/internal/**` prefix. **Those endpoints had a path convention, but no actual enforcement.** Any client that could reach User service on the network — another container, a compromised pod, a curious developer on the same Docker network — could call `GET /internal/users?ids=...` and read every profile in the system, with nothing checking who was asking. The `/internal` prefix was a *label*, not a *boundary*.

This is a well-known trap in early microservice migrations: the public-facing boundary (browser → Nginx → service) gets secured first because it's the obvious attack surface, and the service-to-service boundary gets forgotten because "it's all inside our own Docker network anyway." That assumption breaks the moment you have multiple teams, a shared cluster, or a network policy misconfiguration — internal doesn't mean trusted.

Phase 5 makes `/internal/**` actually mean something: **only Auth and Chat, authenticated with a distinct service credential, may call it.**

---

## Design: a second, separate JWT — not the user's token

The tempting shortcut is to have Auth/Chat just forward the *user's* JWT when calling User internally. Rejected, for two reasons:

1. **Wrong semantics.** `POST /internal/users` (create profile during registration) has no authenticated user yet — the user doesn't exist until this call succeeds. There is no user JWT to forward.
2. **Blast radius.** If the user-token secret ever leaked, an attacker could mint tokens that pass through *every* boundary in the system — public endpoints and internal ones. Two secrets means a leak of one only compromises one surface.

So Phase 5 introduces a **second HS256 secret** (`app.service-jwt.secret`), completely independent from `app.jwt.secret`. Auth, User, and Chat all hold this shared secret (it's symmetric — same trust model as the user-JWT secret already had between Auth-as-issuer and User/Chat-as-verifiers). Tokens signed with it:

- carry `sub` = the calling service's name (`spring.application.name` — `auth-service` or `chat-service`)
- carry `scope: internal`
- carry `iss: the-chat-internal`
- expire in ~60 seconds (`app.service-jwt.ttl`)

Short TTL is deliberate: these tokens are minted fresh per outgoing call (HMAC signing is cheap — microseconds), not cached, so a token captured in a proxy log or error trace is useless within a minute.

---

## New components (all in `common`, reused by every service)

| Component | Role |
|---|---|
| `ServiceJwtProperties` | Binds `app.service-jwt.{issuer,secret,ttl}` |
| `ServiceJwtConfig` | Provides `serviceJwtEncoder` / `serviceJwtDecoder` beans, HS256, signed/verified with the service secret |
| `ServiceTokenIssuer` | Mints a short-lived service token stamped with this service's own name as `sub` |
| `ServiceAuthRequestInterceptor` | A `ClientHttpRequestInterceptor` — attaches `Authorization: Bearer <service-token>` to every outgoing request on a `RestClient` it's wired into |

Every service already had its own `jwtEncoder`/`jwtDecoder` beans (for user tokens). Adding a second pair for service tokens created **bean ambiguity** — Spring couldn't tell which `JwtEncoder` to inject where. Resolved with:

- `@Primary` on each service's original user-token `JwtEncoder`/`JwtDecoder` (so unqualified injections — controllers, filters that only care about user tokens — keep working unchanged)
- `@Qualifier("serviceJwtEncoder")` / `@Qualifier("serviceJwtDecoder")` at the few injection points that specifically need the service variant (`ServiceTokenIssuer`, User's internal filter chain)

This is the standard Spring pattern for "two beans of the same type in the context, one is the sensible default": mark the default `@Primary`, qualify the exception.

---

## Wiring it into the callers

`UserProfileClient` (Auth) and `UserServiceClient` (Chat) both build their `RestClient` the same way now:

```java
this.restClient = restClientBuilder
        .baseUrl(userServiceBaseUrl)
        .requestInterceptor(serviceAuthRequestInterceptor)
        .build();
```

Neither client's calling code changed at all — `createProfile(...)`, `batchGetByIds(...)`, `ensureFriendship(...)` still just call `restClient.post()...retrieve()...`. The interceptor is transparent: it runs on every request through that `RestClient` instance, stamping the header before the request leaves the JVM. This is the same shape as an Axios request interceptor on the frontend, or a `Retrofit`/`OkHttp` interceptor — cross-cutting concerns belong at the transport layer, not scattered into every call site.

---

## Enforcing it on the callee: two security filter chains on User service

Before Phase 5, User service had one `SecurityFilterChain` validating user JWTs for everything. Now it has two, ordered:

```java
@Bean
@Order(1)
SecurityFilterChain internalSecurityFilterChain(
        HttpSecurity http,
        @Qualifier("serviceJwtDecoder") JwtDecoder serviceJwtDecoder) throws Exception {
    return http
            .securityMatcher("/internal/**")
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/internal/**").hasAuthority("SCOPE_internal")
                    .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2
                    .bearerTokenResolver(new DefaultBearerTokenResolver())
                    .jwt(jwt -> jwt.decoder(serviceJwtDecoder)))
            .build();
}

@Bean
@Order(2)
SecurityFilterChain publicSecurityFilterChain(...) {
    return http
            // everything else — validates end-user JWTs, unchanged from Phase 3/4
            ...
}
```

Two things worth calling out because both bit us during implementation:

1. **`securityMatcher` + `@Order`, not one chain with two auth rules.** Spring Security evaluates chains in order and uses the *first one whose `securityMatcher` matches*. `/internal/**` requests never even reach the public chain's JWT decoder, and vice versa. Trying to do this in a single chain with conditional logic gets unreadable fast — two small, single-purpose chains are far easier to reason about and audit.

2. **`BearerTokenResolver` is a shared bean by default.** User service's public chain reads the user JWT from a cookie (`CookieBearerTokenResolver`, needed because the browser never sends `Authorization` headers to it — the JWT lives in an httpOnly cookie set by Auth). Without overriding it, the *internal* chain silently inherited that same cookie resolver — so a request with `Authorization: Bearer <service-token>` and no cookie was treated as having no token at all, falling through to `AnonymousAuthenticationFilter`. The fix: explicitly give the internal chain its own `new DefaultBearerTokenResolver()`, which reads the standard `Authorization` header — the mechanism every HTTP client (including `RestClient`) uses natively via `setBearerAuth(...)`.

---

## What this actually prevents

```
                     ┌─────────────────┐
   Auth service ─────┤  service token  ├─────►  /internal/**  (SCOPE_internal required)
   Chat service ─────┤  (60s TTL,      │             │
                     │   own secret)    │             ▼
                     └─────────────────┘        User service
                                                       │
   Anything else ────────X 401 X──────────────────────┘
   (curl, another
    container, a
    misconfigured
    service)
```

Before: anyone who could route to `user-service:8084` could read/write internal user data.
After: only holders of the service secret — Auth and Chat, and only for ~60 seconds per token — can reach `/internal/**`. A stolen service token is useless within a minute. A stolen user token (browser cookie theft) still can't reach `/internal/**` at all, because the internal chain doesn't accept `SCOPE_internal`-less tokens regardless of who issued them.

---

## Verified

```powershell
# 1. Direct, unauthenticated call to the internal endpoint on User's own exposed port
GET http://localhost:8084/internal/users?ids=...
→ 401 Unauthorized                      # was previously 200 before Phase 5

# 2. Health check on the same service — still public, unaffected
GET http://localhost:8084/health
→ 200 OK

# 3. End-to-end register through Nginx (exercises Auth → User with a real service token)
POST http://localhost:8080/api/v1/auth/register
→ 201 Created                           # register saga: credential written, then
                                         # UserProfileClient.createProfile() succeeds
                                         # against the now-enforced /internal/users

# 4. Login + list conversations through Nginx (Chat's own user-JWT auth, unaffected)
POST http://localhost:8080/api/v1/auth/login   → 200 OK
GET  http://localhost:8080/api/v1/conversations → 200 OK
```

`Chat → User` (`ensureFriendship`, `batchGetByIds`) uses the exact same `ServiceAuthRequestInterceptor` + `RestClient` pattern as `Auth → User`, verified above — the mechanism is identical code, not a separate implementation to re-prove per caller.

Unit coverage: `ServiceTokenIssuerTest` (in `common`) round-trips a token through `ServiceTokenIssuer` → `serviceJwtDecoder`, asserting `sub`, `scope`, `iss`, and expiry, and asserts a token signed with a *different* secret is rejected — the actual security property this whole phase exists to guarantee.

---

## Lessons (the two bugs worth remembering)

1. **A naming convention (`/internal/**`) is not a security control.** It's a hint to developers; it does nothing to a network-level caller. If a boundary matters, put an `authorizeHttpRequests` rule on it, not just a package/path name.
2. **`BearerTokenResolver`, `JwtDecoder`, `JwtEncoder` are all just beans — they don't know which "chain" they conceptually belong to.** Every additional filter chain or credential type you add can silently poison ones that already worked, purely through Spring's default bean-wiring (unqualified injection picks *a* matching bean, not necessarily the one you meant). The fix pattern is always the same: name/qualify explicitly at the injection point, and use `@Primary` for the one true default. Whenever behavior for a specific chain looks like it's "falling through" to defaults, suspect bean resolution before suspecting the security rule itself.

---

## Teach it

### Internal (5-min whiteboard)
Draw the two-secret diagram above. Ask: "Auth's user-JWT secret leaks. What's compromised?" (public endpoints, User's public chain — but *not* `/internal/**`, because that requires the other secret). Then: "The service-JWT secret leaks instead. What's compromised?" (only `/internal/**`, for at most ~60 seconds per stolen token, and never the public login flow). This is the concrete payoff of **secret separation by trust boundary**, not just "more secrets = more secure."

### External (blog post seed)
Title: *"The internal API nobody secured — a service-to-service auth retrofit"*
Angle: most microservices tutorials show the public JWT flow and stop. The internal East-West traffic is where real incidents happen (SSRF, lateral movement inside a cluster). Walk through: recognizing the gap → choosing symmetric service-JWTs over forwarding user tokens (and why) → the two Spring Security bean-wiring bugs that make this genuinely tricky to get right the first time, not because the concept is hard but because of default bean resolution.

---

## Exit criteria (all pass)

- [x] `common` module: `ServiceJwtProperties`, `ServiceJwtConfig`, `ServiceTokenIssuer`, `ServiceAuthRequestInterceptor`, unit-tested
- [x] `Auth → User` and `Chat → User` clients both attach service tokens automatically via `RestClient` interceptor
- [x] User service: `/internal/**` requires `SCOPE_internal`, rejects everything else (verified `401` on direct unauthenticated call)
- [x] User service: public endpoints (`/health`, end-user routes) unaffected by the new chain
- [x] End-to-end register (Auth → User internal call) succeeds through Nginx: `201 Created`
- [x] End-to-end login + list conversations (Chat's own user-JWT path) unaffected: `200 OK`
- [x] `.\gradlew.bat :common:test` passes (`ServiceTokenIssuerTest`)
- [x] All services rebuilt, redeployed via `docker compose`, verified live
