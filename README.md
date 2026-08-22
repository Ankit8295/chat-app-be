# The Chat Backend

Gradle multi-project monorepo for Auth, User, and Chat microservices — Java 21, Spring Boot 4, PostgreSQL (DB per service), Redis, JWT, Flyway.

## Structure

```text
chat-app-be/
├── services/          # Independently deployable Spring Boot apps
│   ├── auth/          # Credentials, JWT issuance, refresh sessions
│   ├── user/          # Profiles, friends, preferences + /internal/**
│   └── chat/          # Conversations, messages, WebSocket + Redis fan-out
├── libs/
│   └── common/        # Shared library (NOT deployed) — JWT/service-auth, ApiError, etc.
├── infra/
│   ├── docker/        # Per-service Dockerfiles
│   ├── nginx/         # API gateway (local + nginx.prod.conf)
│   ├── postgres/      # Prod multi-DB init script
│   ├── scripts/       # Backup helpers
│   ├── compose.yml    # Local orchestration
│   └── compose.prod.yml
├── docs/
│   ├── architecture/
│   └── DEPLOY.md      # Production checklist (AWS / Cloudflare / GitHub)
├── settings.gradle    # Project includes + projectDir remaps
├── build.gradle       # Shared Java 21 / Spring BOM / version for all modules
└── gradlew.bat
```

`libs/common` is compiled **into** each service JAR at build time. It has no container of its own.

Gradle project names stay `:auth`, `:user`, `:chat`, `:common` (see `settings.gradle`), so commands did not change after the folder move.

## Local Setup

Build fat JARs, then start everything via Compose:

```powershell
.\gradlew.bat :auth:bootJar :user:bootJar :chat:bootJar -x test
docker compose -f infra/compose.yml up --build
```

Or run a single service with Gradle (DBs/Redis must already be up):

```powershell
.\gradlew.bat :auth:bootRun
.\gradlew.bat :user:bootRun
.\gradlew.bat :chat:bootRun
```

API gateway: `http://localhost:8080`

## Production

See **[docs/DEPLOY.md](docs/DEPLOY.md)** for AWS Lightsail + Cloudflare + Vercel steps.

```bash
cp .env.production.example .env   # fill secrets on the server only
docker compose -f infra/compose.prod.yml --env-file .env up -d --build
```

## Environment

Copy values from `.env.production.example` (prod) or your local `.env` and replace secrets before production. Each service also has defaults under `services/*/src/main/resources/application.yml`.

Notable secrets (must match across services that share them):

- `JWT_SECRET` — end-user access tokens (Auth issues; User/Chat verify)
- `SERVICE_JWT_SECRET` — service-to-service tokens for User `/internal/**` (separate from `JWT_SECRET`)
- `AUTH_COOKIE_DOMAIN` — e.g. `.ankitdev.in` so cookies are shared by `chat.` and `api.` subdomains
- `REDIS_PASSWORD` — required in production compose

## Auth Routes

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "User",
  "password": "password123"
}
```

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Both routes set an HTTP-only `access_token` cookie.

Logout:

```http
POST /api/v1/auth/logout
```

## Further reading

- [Phase 5 — service-to-service auth](docs/architecture/phase-5-service-auth.md)
- [Phase 6 — monorepo layout](docs/architecture/phase-6-monorepo-layout.md)
