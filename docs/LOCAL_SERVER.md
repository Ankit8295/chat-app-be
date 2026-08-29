# Local Home Server Guide (Windows PC)

Run the chat backend on your Windows PC using **Docker Desktop** and **Cloudflare Tunnel**. No router port forwarding, no AWS VM. Downtime when the PC is off is acceptable.

**Domain setup:** `api.ankitdev.in` → Cloudflare Tunnel → nginx → Spring Boot services.

---

## Architecture

```text
Frontend (Vercel chat.ankitdev.in or localhost:3000)
        |
        v
   Cloudflare Edge (HTTPS)
        |
        v
   cloudflared container (outbound only)
        |
        v
   nginx :8080 (internal HTTP)
        |
   +----+----+----+
   |    |    |    |
 auth user chat
   |    |    |
   +----+----+
        |
   auth-postgres  user-postgres  chat-postgres
   Redis (refresh tokens + chat pub/sub)
        |
   Cloudflare R2 (profile images — external)
```

Nothing is published to your home router. Postgres and Redis are internal to Docker only.

---

## Prerequisites

1. **Docker Desktop** for Windows (WSL2 backend recommended)
   - Settings → General → **Start Docker Desktop when you sign in**
2. **JDK 21** (to build JARs locally): `.\gradlew.bat bootJar`
3. **Git** (clone this repo)
4. **Cloudflare account** with `ankitdev.in` on Cloudflare DNS
5. **Cloudflare R2** bucket (already set up for profile images)

---

## One-time setup

### 1. Environment file

From `chat-app-be/`:

```powershell
copy .env.home.example .env
```

Edit `.env` and replace every `REPLACE_*` value. Generate secrets:

```powershell
# Run several times for different secrets
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

Or use OpenSSL if installed: `openssl rand -base64 48`

Required:
- `CLOUDFLARE_TUNNEL_TOKEN`
- All DB / Redis / JWT secrets
- `R2_*` values (reuse from your existing Cloudflare R2 setup)

**Never commit `.env`.**

### 2. Cloudflare Tunnel

1. Cloudflare dashboard → **Zero Trust** → **Networks** → **Tunnels**
2. **Create a tunnel** → name e.g. `home-pc-the-chat`
3. Choose **Docker** as connector → copy the **token**
4. Paste into `.env` as `CLOUDFLARE_TUNNEL_TOKEN=...`
5. **Public Hostname:**
   - Subdomain: `api`
   - Domain: `ankitdev.in`
   - Service type: HTTP
   - URL: `http://nginx:8080` (Docker service name — **not** localhost)
6. Remove any old **A record** for `api.ankitdev.in` pointing to AWS Lightsail
7. SSL/TLS mode: **Full** (not strict — no origin cert on home PC)

WebSocket path `/ws` works through the tunnel for chat.

### 3. First start

```powershell
cd C:\Users\Ankit\Documents\GitHub\chat\chat-app-be
.\infra\scripts\start.ps1
```

This builds JARs, starts all containers, and connects the tunnel.

Verify:

```powershell
.\infra\scripts\health-check.ps1
```

Open `https://api.ankitdev.in/health` in a browser (should return chat service health text).

---

## Daily operations

| Task | Command |
|------|---------|
| Start | `.\infra\scripts\start.ps1` |
| Start (skip JAR build) | `.\infra\scripts\start.ps1 -SkipBuild` |
| Stop | `.\infra\scripts\stop.ps1` |
| Restart | `.\infra\scripts\restart.ps1` |
| Logs (all) | `.\infra\scripts\logs.ps1` |
| Logs (one service) | `.\infra\scripts\logs.ps1 -Service auth-service` |
| Health check | `.\infra\scripts\health-check.ps1` |
| Backup DBs | `.\infra\scripts\backup-db.ps1` |
| Restore DB | `.\infra\scripts\restore-db.ps1 -File "C:\Users\You\Backups\the-chat\auth_db_....sql.gz"` |
| Rebuild after code change | `.\infra\scripts\rebuild.ps1` |

Manual compose (equivalent):

```powershell
docker compose -f infra/compose.home.yml --env-file .env up -d --build
docker compose -f infra/compose.home.yml --env-file .env ps
docker compose -f infra/compose.home.yml --env-file .env logs -f
```

---

## Boot sequence (Windows reboot)

```text
Windows login
  → Docker Desktop auto-starts
  → Containers restart (restart: unless-stopped)
  → Postgres + Redis healthchecks pass
  → auth / user / chat JVMs start
  → nginx starts
  → cloudflared reconnects tunnel
  → https://api.ankitdev.in available
```

No manual terminal needed if Docker Desktop auto-start is enabled.

Optional: Task Scheduler runs `start.ps1` at login as a backup.

---

## Persistent storage

Database data lives in Docker volumes `auth_postgres_data`, `user_postgres_data`, and `chat_postgres_data`.

| Command | Data safe? |
|---------|------------|
| `docker compose down` | Yes |
| `.\infra\scripts\stop.ps1` | Yes |
| PC reboot | Yes |
| `docker compose down -v` | **NO — destroys database** |

---

## Backups

### Create backup

```powershell
.\infra\scripts\backup-db.ps1
```

Default output: `%USERPROFILE%\Backups\the-chat\`  
Files: `auth_db_*.sql.gz`, `user_db_*.sql.gz`, `chat_db_*.sql.gz`

### Restore backup

```powershell
.\infra\scripts\restore-db.ps1 -File "C:\Users\You\Backups\the-chat\auth_db_20260829T120000Z.sql.gz"
```

Type `YES` to confirm. Restores one database at a time.

### Schedule nightly backup

1. Open **Task Scheduler** → Create Basic Task
2. Trigger: Daily 2:00 AM
3. Action: Start a program
   - Program: `powershell.exe`
   - Arguments: `-ExecutionPolicy Bypass -File "C:\...\chat-app-be\infra\scripts\backup-db.ps1"`

Copy backups to external drive or cloud storage periodically — do not rely only on the same SSD.

---

## Frontend connection

Set in Vercel (or local `.env`):

```env
NEXT_PUBLIC_BACKEND_API_URL=https://api.ankitdev.in
```

CORS and cookies in `.env` must include:
- `CORS_ALLOWED_ORIGINS=https://chat.ankitdev.in,http://localhost:3000`
- `AUTH_COOKIE_DOMAIN=.ankitdev.in`
- `AUTH_COOKIE_SAME_SITE=None`
- `AUTH_COOKIE_SECURE=true`

**Note:** Next.js `proxy.ts` route guards on `localhost:3000` may not see cookies scoped to `.ankitdev.in`. API calls and WebSocket still work via `withCredentials`.

---

## Security checklist

- [ ] Postgres not exposed on host ports
- [ ] Redis not exposed on host ports
- [ ] No router port forwarding for this app
- [ ] `.env` and `CLOUDFLARE_TUNNEL_TOKEN` never committed
- [ ] CORS limited to `chat.ankitdev.in` and `localhost:3000`
- [ ] Auth cookies: Secure, HttpOnly, SameSite=None, Domain=.ankitdev.in
- [ ] HTTPS externally via Cloudflare
- [ ] Named Docker volume for Postgres
- [ ] Backups stored outside container filesystem
- [ ] Do **not** use `infra/compose.yml` on an internet-facing machine (it publishes DB ports)

---

## Troubleshooting

### Docker not running

```
Error: Docker is not running. Start Docker Desktop first.
```

Open Docker Desktop and wait until the engine is ready.

### Tunnel disconnected

```powershell
docker compose -f infra/compose.home.yml --env-file .env logs cloudflared
```

Check `CLOUDFLARE_TUNNEL_TOKEN` in `.env`. Recreate tunnel token if expired.

### API not reachable externally

1. `.\infra\scripts\health-check.ps1`
2. Confirm Cloudflare tunnel public hostname points to `http://nginx:8080`
3. Confirm `api.ankitdev.in` DNS is CNAME to tunnel (not old Lightsail A record)

### CORS / login fails

- Verify `CORS_ALLOWED_ORIGINS` includes your FE origin
- Cookies require `SameSite=None` + `Secure=true` for cross-origin
- Check browser devtools → Network → login response `Set-Cookie` headers

### Port conflict on host

Home compose publishes **no host ports**. If you still see conflicts, another stack may be running:

```powershell
docker ps
docker compose -f infra/compose.yml down   # stop local dev stack if running
```

### Container won't start / Flyway error

```powershell
.\infra\scripts\logs.ps1 -Service backend-1
```

Each Postgres container auto-creates its database and user on first boot (no init script). If volumes are corrupt and you accept data loss:

```powershell
docker compose -f infra/compose.home.yml --env-file .env down -v
.\infra\scripts\start.ps1
```

---

## Scaling later (optional)

The app keeps Redis pub/sub and `INSTANCE_ID` for multi-instance chat. To add a second chat container:

1. Copy `backend-1` block in `compose.home.yml` as `backend-2` with `SPRING_FLYWAY_ENABLED=false`
2. Uncomment `backend-2:8082` and `ip_hash` in `nginx.home.conf`
3. `docker compose up -d --build`

No Java code changes required.

---

## Files reference

| File | Purpose |
|------|---------|
| `infra/compose.home.yml` | Home server stack |
| `infra/nginx/nginx.home.conf` | Internal HTTP gateway |
| `.env.home.example` | Env template |
| `infra/scripts/*.ps1` | Operator scripts |
| `docs/DEPLOY.md` | AWS Lightsail path (separate) |

---

## Manual steps checklist

- [ ] Docker Desktop installed + auto-start enabled
- [ ] `.env` created from `.env.home.example`
- [ ] Cloudflare Tunnel created + token in `.env`
- [ ] `api.ankitdev.in` DNS points to tunnel
- [ ] R2 credentials in `.env`
- [ ] First `start.ps1` successful
- [ ] Login + WebSocket tested from FE
- [ ] Backup + restore tested once
- [ ] Nightly backup scheduled (optional)
- [ ] AWS Lightsail stopped/cancelled when satisfied
