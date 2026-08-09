# DIY practice backlog — after Phase 6

**Goal:** Implement these yourself to practice the microservices seams you already built (Auth / User / Chat, service JWT, batch ACL, WS+Redis, monorepo).  
**Rule:** Stay inside existing services unless a task explicitly says otherwise. Prefer `libs/` for shared helpers (e.g. S3), not a new deployable.

**Suggested order:** T1 → T2 → T3 → T4 → T5 → pick one of T6–T8 → run drills D1–D3.

---

## T1 — Update user profile (text)

**Practices:** User owns profile; public cookie JWT API; FE ↔ Nginx ↔ User; i18n + Typography.

### Expected (backend — User service)
- [ ] `PATCH /api/v1/users/me` (or `PUT`) accepting e.g. `{ "name": "..." }` (optional `image` later in T3).
- [ ] Zod/validation mirror: name length same as register/create profile rules.
- [ ] Only the authenticated user (`jwt.userId`) can update themselves.
- [ ] Response: updated `UserResponse` (id, email, name, image).
- [ ] No Auth/Chat code changes; no cross-DB writes.

### Expected (frontend)
- [ ] Settings profile tab: editable name + save.
- [ ] React Query: mutation invalidates/refetches `me` / search cache as needed.
- [ ] All labels via `messages/en.ts` + `useTranslations`; text via `Typography`.

### Done when
- Logged-in user changes name → `/me` and sidebar/search show new name after refresh/invalidate.
- Unauthenticated → 401; invalid name → 400 with field errors.

---

## T2 — Update group info (name / about)

**Practices:** Chat owns conversations; participant authz; soft UUIDs unchanged.

### Expected (backend — Chat service)
- [ ] `PATCH /api/v1/conversations/{id}` for `GROUP` only (reject DIRECT or return 400).
- [ ] Body e.g. `{ "name"?: string, "about"?: string }` with validation.
- [ ] Caller must be a participant (same rule as get conversation); optionally only `createdBy`.
- [ ] Persist on `conversations` table; return updated detail/list DTO.
- [ ] Optional stretch: publish a WS/realtime event so other members see the rename live.

### Expected (frontend)
- [ ] Group info UI: edit name/about + save.
- [ ] Update conversation query cache / list title without full page reload.

### Done when
- Member updates group → list + detail show new name/about.
- Non-member → 404/403; DIRECT conversation → rejected.

---

## T3 — Shared S3 lib + profile image

**Practices:** `libs/` vs `services/`; never put binaries in Postgres; browser → S3 direct upload.

### Expected (`libs/s3` or `libs/media`)
- [ ] Gradle module depended on by User (and later Chat).
- [ ] Config: bucket, region, credentials via env (no secrets in repo).
- [ ] API: build object key + create **presigned PUT** (and optionally GET) URL.
- [ ] Key shape e.g. `users/{userId}/avatar/{uuid}.{ext}`.

### Expected (User service)
- [ ] `POST /api/v1/users/me/avatar/presign` → `{ uploadUrl, objectKey, publicUrl? }` after authz.
- [ ] Allowlist content types (e.g. jpeg/png/webp) + max size documented in response or config.
- [ ] `PATCH /me` (or confirm endpoint) saves final image URL/key on `app_users.image`.

### Expected (frontend)
- [ ] Pick file → call presign → `PUT` to S3 → save URL on profile.
- [ ] Show avatar from URL; loading/error states (i18n).

### Expected (AWS / local)
- [ ] Bucket CORS allows `PUT` from `http://localhost:3000`.
- [ ] Bucket private; read via CloudFront or short-lived GET presign (document which you chose).

### Done when
- Avatar uploads without sending file bytes through Spring.
- Wrong user cannot get a presign for another user’s prefix.
- Chat/Auth unchanged except consuming image URL from existing profile batch fetch.

---

## T4 — Group image (reuse T3 lib)

**Practices:** Same S3 pattern; Chat authz; Chat DB owns `conversations.image`.

### Expected (Chat service)
- [ ] Depends on `libs/s3`.
- [ ] `POST /api/v1/conversations/{id}/image/presign` — participant/creator only; key prefix `chat/{conversationId}/...`.
- [ ] Persist image URL/key on group conversation (PATCH or confirm endpoint).

### Expected (frontend)
- [ ] Group info: upload/change image; list/header show new image.

### Done when
- Group image updates for all members on next fetch (or live via WS if you added T2 stretch).
- Non-member cannot presign.

---

## T5 — Message attachments

**Practices:** Chat domain modeling; metadata in Chat DB; S3 for bytes; WS payload may include attachment refs.

### Expected (backend — Chat)
- [ ] Flyway: e.g. `message_attachments (id, message_id, object_key, content_type, size_bytes, created_at)` or embed JSON on message — pick one and document.
- [ ] Presign: `POST /api/v1/conversations/{id}/attachments/presign` (participant only).
- [ ] Send message flow accepts attachment key(s) **only if** key prefix matches that conversation (prevent open upload abuse).
- [ ] History + realtime `MessageResponse` includes attachment metadata (url or key + type + name).

### Expected (frontend)
- [ ] Attach file in composer → presign → S3 PUT → send message with refs.
- [ ] Bubble shows link/preview for image/file; i18n for errors (size/type).

### Done when
- Attachment survives refresh (loaded from Chat DB + S3).
- User cannot attach a key from another conversation’s prefix.

---

## T6 — Remove friend / unfriend (User)

**Practices:** Friendship stays in User; Chat must not own friendship tables.

### Expected (backend — User)
- [ ] e.g. `DELETE /api/v1/users/friends/{friendUserId}` or `POST .../unfriend`.
- [ ] Idempotent if already not friends.
- [ ] Friends list no longer returns that user.

### Expected (frontend)
- [ ] Friends tab: remove action + confirm; list updates.

### Done when
- Unfriend works; existing DIRECT chat in Chat **still exists** (soft UUID — correct). Optional: product copy “not friends” later; don’t delete Chat data from User.

---

## T7 — Typing indicator (Chat + WS)

**Practices:** Existing WS + Redis fan-out; no new infra.

### Expected (backend — Chat)
- [ ] WS event type e.g. `TYPING` with `{ conversationId, userId }` (no DB required, or short Redis TTL).
- [ ] Fan-out to conversation participants on other instances via existing realtime channel.

### Expected (frontend)
- [ ] On keypress (debounced): send typing event.
- [ ] Show “X is typing…” in conversation header/thread; clear after timeout.

### Done when
- Two browsers (ideally hitting different Chat instances) see typing state.

---

## T8 — List / revoke sessions (Auth)

**Practices:** Auth owns refresh tokens in Redis; User/Chat stay out.

### Expected (backend — Auth)
- [ ] `GET /api/v1/auth/sessions` — list refresh sessions for current user (device/created/last used if you store them).
- [ ] `DELETE /api/v1/auth/sessions/{id}` or revoke-all-except-current.
- [ ] Revoked refresh cannot call `/refresh`.

### Expected (frontend)
- [ ] Settings security section: list sessions + revoke.

### Done when
- Revoke on device A forces re-login on device B after access token expires (or immediately if you also clear access — document behavior).

---

## Drills (no feature — verify architecture)

| ID | Action | Expected |
|---|---|---|
| **D1** | `docker compose stop user-service` then register | Auth fails predictably (4xx/5xx); no half-created login without profile (or compensated). |
| **D2** | Same stop; list conversations / open chat | Chat still responds; names/images may be empty — app does not hard-crash. |
| **D3** | `curl` User `:8084/internal/users?...` without service Bearer | **401**; with cookie-only user JWT still **401** on `/internal/**`. |

---

## Explicitly out of scope for this backlog

- New **Media** microservice (until T3–T5 feel duplicated/painful).
- Spring Cloud Gateway, Kafka, Resilience4j/outbox, OpenTelemetry, K8s.
- Storing file bytes in Postgres.

---

## How to use this plan

1. Pick **one** task (start T1).  
2. Before coding: write the request/response JSON and which service owns the table.  
3. Implement BE → smoke via Nginx → FE → i18n.  
4. Check the **Done when** box; then move on.

If you want the next Cursor session to implement a task with you, say e.g. “implement T1” or “implement T3”.
