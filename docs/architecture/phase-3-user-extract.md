# Phase 3 — User Service Extraction

**Status:** Implemented  
**Date:** 2026-08-08

---

## What changed

| Area | Before (Phase 2) | After (Phase 3) |
|---|---|---|
| User topology | Module inside monolith (`app` JAR) | **Standalone Spring Boot app** on port 8084 |
| User DB | Shared `the_chat` Postgres | **Own `user_db` Postgres** |
| Chat ↔ User | JPA `@ManyToOne AppUser` in entities | **HTTP batch call** `GET /internal/users?ids=` |
| Friendship write | `FriendshipRepository.save()` inside Chat | Chat calls `POST /internal/friendships/ensure` on User |
| Cross-DB FKs | `conversation_participants.user_id` → `app_users` | **Dropped** (V9 migration) — soft UUID only |
| Remaining monolith | Auth + User + Chat assembled as one JAR | **Chat + Common only** |
| Nginx routing | `/api/v1/auth/` → auth; everything → backend | Added `/api/v1/users/` → user-service |

---

## The core problem Phase 3 solves: the network N+1

In a monolith, loading 50 messages with sender names costs **1 SQL query** (join).

After splitting User into its own service, a naïve port of that logic would be:
```
for each message:
    GET /internal/users/{senderId}   ← 50 HTTP round-trips
```
This is the **network N+1 problem** — the distributed equivalent of the ORM N+1 anti-pattern.

### The fix: batch endpoint

```
GET /internal/users?ids=uuid1,uuid2,uuid3   ← 1 HTTP round-trip for 50 senders
```

`UserServiceClient.batchGetByIds()` collects all unique IDs first, fires one call, then joins in-memory:

```java
// MessageService.getMessages
Set<UUID> senderIds = page.stream().map(Message::getSenderId).collect(Collectors.toSet());
Map<UUID, UserProfile> profileMap = userServiceClient.batchGetByIds(List.copyOf(senderIds));
List<MessageResponse> items = page.stream()
        .map(m -> MessageResponse.from(m, profileMap.get(m.getSenderId())))
        .toList();
```

---

## The key pattern: Anti-Corruption Layer (ACL)

Chat's `com.thechat.user.UserProfile` is NOT the same class as the User service's `AppUser` or `UserResponse`.
It's Chat's **own local representation** of the data it needs from User.

```
User service                Chat service
─────────────              ──────────────
AppUser.java               com.thechat.user.UserProfile.java
UserResponse.java          (Chat's ACL DTO — owns its own model)
```

Why this matters:
- User can add/rename fields without breaking Chat (Chat only maps what it needs)
- Chat is never coupled to User's internal domain model
- Named pattern: **Anti-Corruption Layer** (ACL) — DDD term for the translation boundary

---

## Entity changes — the "no cross-DB FK" rule

Before (JPA FK):
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private AppUser user;   // FK to app_users in same DB
```

After (soft UUID):
```java
@Column(name = "user_id", nullable = false)
private UUID userId;    // plain UUID — no DB-level FK to user_db
```

**Why?** In database-per-service, `conversation_participants.user_id` and `app_users.id` live in different Postgres instances. You cannot have a FK between databases. The UUID is a "soft reference" — the application maintains consistency, not the DB.

**What you give up:** Cascading deletes and DB-level referential integrity.  
**What you gain:** Independent deployment, independent scaling, no coupling at the data tier.

---

## Friendship write: from Chat to User

Before (Chat wrote directly to its DB):
```java
// ConversationService (Phase 2)
friendshipRepository.save(new Friendship(u1, u2, ACTIVE));
```

After (Chat delegates to User via HTTP):
```java
// ConversationService (Phase 3)
userServiceClient.ensureFriendship(currentUserId, targetUserId);
```

**Why move it?** Friendship is part of the social graph — User's domain. Chat merely triggers it as a side effect of opening a direct conversation. Keeping the write in Chat created a cross-boundary violation.

**Trade-off:** If User service is down, `ensureFriendship` fails silently (logged as WARN). The conversation still opens successfully — eventual consistency. This is an explicit design choice for Phase 3; Phase 6 adds a proper idempotent retry / outbox.

---

## JPQL changes: removing the join-fetched `AppUser`

Old queries joined across to `app_users` (same DB):
```sql
SELECT DISTINCT c FROM Conversation c
JOIN FETCH c.participants p
JOIN FETCH p.user u          ← crosses to app_users table
WHERE c.id = :conversationId
```

New queries only join within the chat DB:
```sql
SELECT DISTINCT c FROM Conversation c
JOIN FETCH c.participants p  ← stays within chat DB
WHERE c.id = :conversationId
```

After fetching conversations, `getUserConversations` does:
1. Collect all participant `userId` UUIDs from the loaded entities
2. One batch HTTP call: `userServiceClient.batchGetByIds(allUserIds)`
3. Build responses by joining the in-memory `Map<UUID, UserProfile>`

---

## DB migration V9

```sql
ALTER TABLE conversation_participants DROP CONSTRAINT conversation_participants_user_id_fkey;
ALTER TABLE messages                  DROP CONSTRAINT messages_sender_id_fkey;
ALTER TABLE conversations             DROP CONSTRAINT conversations_created_by_fkey;
```

These drop the Postgres-level FK constraints. The columns (`user_id`, `sender_id`, `created_by`) remain as plain `UUID` columns. Existing data is preserved.

---

## Service topology after Phase 3

```
Browser
  │
  ▼
Nginx :8080
  ├── /api/v1/auth/*    → Auth Service    :8083  (auth_db)
  ├── /api/v1/users/*   → User Service    :8084  (user_db)
  ├── /ws               → Chat Monolith   :8081/:8082 (the_chat DB)
  └── /api/v1/*         → Chat Monolith   :8081/:8082

Chat ──────(HTTP)──────► User  (batch profile + friendship writes)
Auth ──────(HTTP)──────► User  (register saga: create/delete profile)
```

---

## How to run locally (Phase 3)

```powershell
# Terminal 1 — Auth service
cd chat-app-be
.\gradlew.bat :auth:bootRun

# Terminal 2 — User service
cd chat-app-be
.\gradlew.bat :user:bootRun

# Terminal 3 — Chat monolith (app module, Chat + Common)
cd chat-app-be
.\gradlew.bat :app:bootRun

# Or via compose:
.\gradlew.bat :auth:bootJar :user:bootJar :app:bootJar -x test
docker compose up
```

---

## Teach it

### Internal (5-min whiteboard)
Draw four boxes: **Browser**, **Chat**, **User**, **Auth**.  
Ask: "When Chat shows a conversation list with 10 conversations, each with 2 participants — how many HTTP calls to User?" → 1 batch call. Show the old N+1 path vs the batch path. Then ask: "What if User is down while Chat is loading messages?" → names are null, app degrades gracefully.

### External (blog post seed)
Title: *"Killing the network N+1: batch endpoints when splitting microservices"*  
Angle: everyone talks about the DB N+1. The distributed N+1 is worse (network latency × N). Walk through the Anti-Corruption Layer + batch endpoint pattern.

---

## Exit criteria (all must pass)

- [ ] `.\gradlew.bat :app:test` — `ModuleDependencyTest` passes (chat imports no user module classes)
- [ ] User service starts on port 8084, `POST /internal/users` returns 201
- [ ] Chat service starts on port 8081, `GET /api/v1/conversations` returns conversations with user names
- [ ] V9 migration applied — FK constraints dropped, no Hibernate validation errors
- [ ] Friendship is created when opening a DIRECT conversation via User service call
- [ ] `docker compose up` routes `/api/v1/users/*` to user-service
- [ ] Frontend user/friends/preferences flows still work end-to-end
