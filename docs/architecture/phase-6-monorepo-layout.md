# Phase 6 — Monorepo folder layout

**Status:** Implemented
**Date:** 2026-08-08

---

## What changed

| Area | Before | After |
|---|---|---|
| Deployable modules | Flat at repo root (`auth/`, `user/`, `chat/`) | `services/{auth,user,chat}/` |
| Shared library | Flat `common/` | `libs/common/` |
| Docker / nginx / Compose | Root `Dockerfile.*`, `nginx/`, `compose.yml` | `infra/docker/`, `infra/nginx/`, `infra/compose.yml` |
| Gradle project names | `:auth`, `:user`, `:chat`, `:common` | **Unchanged** (via `projectDir` remaps in `settings.gradle`) |
| Java packages | `com.thechat.*` | **Unchanged** |

No runtime behavior change — this phase is physical layout only.

---

## Why three top-level folders

- **`services/`** — things you build into a Docker image and deploy. One process, one port, one DB ownership story.
- **`libs/`** — code compiled *into* those images. `common` is never deployed alone.
- **`infra/`** — how you run the system locally / in a cluster edge (Compose, Dockerfiles, Nginx). Not business logic.

```text
Build Auth:  services/auth + libs/common  →  auth-*.jar  →  infra/docker/Dockerfile.auth  →  container
```

---

## Gradle map (unchanged coordinates)

| File | Role |
|---|---|
| `settings.gradle` | Declares projects and maps them to `services/` / `libs/` paths |
| Root `build.gradle` | Shared Java 21, Spring BOM, `group`/`version` for all subprojects |
| `libs/common/build.gradle` | Library deps only — no Boot plugin |
| `services/*/build.gradle` | Boot plugin + that service’s starters; `implementation project(':common')` |
| `gradlew` + wrapper | Pinned Gradle version for every machine/CI |

Commands stay the same:

```powershell
.\gradlew.bat :auth:bootJar
.\gradlew.bat :common:test
docker compose -f infra/compose.yml up --build
```

---

## Teach it

**Whiteboard (2 min):** Draw `services` vs `libs` vs `infra`. Ask: “If we change `ServiceTokenIssuer`, which images must we rebuild?” → Auth and Chat (and User if it pulls the decoder path) — because `common` is baked into each JAR at build time, not a live shared process.

---

## Exit criteria

- [x] Modules live under `services/` and `libs/`; packaging under `infra/`
- [x] `settings.gradle` remaps `projectDir`; `:auth:bootJar` still works
- [x] Compose uses repo-root build context + `infra/docker/*` Dockerfiles
- [x] `.\gradlew.bat :auth:bootJar :user:bootJar :chat:bootJar -x test` succeeds
- [x] `docker compose -f infra/compose.yml up --build` brings the stack up; register/login smoke passes
