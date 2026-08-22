# Production deployment guide
#
# Code in this repo is ready for AWS Lightsail + Cloudflare + Vercel.
# This document is the **manual checklist** for things only you can do
# (cloud consoles, DNS, secrets, first boot).

Target stack (see also `infra/compose.prod.yml`):

- AWS Lightsail **Bundle:4GB** (dual-stack), Mumbai — ~$23.54/mo
- Single Postgres (3 DBs) + Redis + auth + user + 1× chat + nginx
  (Redis pub/sub + INSTANCE_ID stay on; add a second chat container later without code changes)
- Domains: `api.ankitdev.in` (API), `chat.ankitdev.in` (Next.js FE)
- Cloudflare DNS/TLS/R2 + Vercel FE

---

## A. AWS Lightsail (do first)

1. Create instance
   - Region: **Asia Pacific (Mumbai)**
   - Blueprint: **Ubuntu 24.04** (or 22.04)
   - Bundle: **4GB** normal dual-stack (NOT `Bundle4gb_ipv6`)
   - Name: e.g. `the-chat-prod`
2. Networking → attach a **static IP** to the instance.
3. Firewall (Lightsail networking): allow **SSH 22**, **HTTP 80**, **HTTPS 443** only. Do **not** open 5432 / 6379 / 808x.
4. Download / add your SSH key; confirm you can SSH:
   ```bash
   ssh -i your-key.pem ubuntu@<STATIC_IP>
   ```

### First-boot hardening (on the instance)

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose-v2 ufw fail2ban
sudo usermod -aG docker ubuntu   # then log out/in
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

# 2 GB swap (required on 4 GB box)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Clone / place the repo (example path used by CI):

```bash
sudo mkdir -p /opt/the-chat && sudo chown ubuntu:ubuntu /opt/the-chat
cd /opt/the-chat
git clone <YOUR_REPO_URL> .
# or rely on GitHub Actions SCP (see section D)
```

---

## B. Cloudflare (DNS + TLS + R2)

### DNS

1. Add `ankitdev.in` to Cloudflare (free plan); update nameservers at your registrar.
2. Records:
   - `A` **api** → Lightsail static IP — **Proxied** (orange cloud)
   - `CNAME` **chat** → Vercel target (from Vercel dashboard) — usually DNS-only or as Vercel instructs
   - `CNAME` / `A` for **www** / apex as needed for your personal site
3. SSL/TLS mode: **Full (strict)** once the origin cert is installed (below).

### Origin Certificate (for nginx on the VPS)

1. Cloudflare → SSL/TLS → Origin Server → **Create certificate**
   - Hostnames: `api.ankitdev.in` (and `*.ankitdev.in` if you want)
   - Validity: 15 years is fine
2. On the Lightsail box:
   ```bash
   mkdir -p /opt/the-chat/chat-app-be/infra/nginx/certs
   nano /opt/the-chat/chat-app-be/infra/nginx/certs/origin.pem   # paste cert
   nano /opt/the-chat/chat-app-be/infra/nginx/certs/origin.key   # paste key
   chmod 600 /opt/the-chat/chat-app-be/infra/nginx/certs/origin.key
   ```

### R2 (profile images)

1. R2 → Create bucket e.g. `the-chat-profile-images`
2. Create API token with Object Read/Write on that bucket
3. Optional: public access / custom domain for `R2_PUBLIC_BASE_URL`
4. Fill `R2_*` in the server `.env` (see section C)
5. Optional second bucket `the-chat-db-backups` for `backup-postgres.sh`

---

## C. Server `.env` + first start

```bash
cd /opt/the-chat/chat-app-be
cp .env.production.example .env
chmod 600 .env
nano .env   # replace ALL REPLACE_* values; generate with: openssl rand -base64 48
```

Critical values:

| Variable | Example / note |
|---|---|
| `JWT_SECRET` / `SERVICE_JWT_SECRET` | Different, each ≥32 chars; same across all services |
| `AUTH_COOKIE_DOMAIN` | `.ankitdev.in` |
| `CORS_ALLOWED_ORIGINS` | `https://chat.ankitdev.in,http://localhost:3000` (local + deployed FE) |
| `AUTH_COOKIE_SAME_SITE` | `None` (required for localhost FE → api.ankitdev.in cookies) |
| `AUTH_COOKIE_SECURE` | `true` |
| DB / Redis passwords | Strong, unique |

Build JARs (if not using CI yet), then start:

```bash
# Needs JDK 21 on the box OR use GitHub Actions to copy jars
./gradlew :auth:bootJar :user:bootJar :chat:bootJar -x test
# Prefer building in CI — Gradle can OOM a 4 GB box

chmod +x infra/postgres/init-databases.sh infra/scripts/backup-postgres.sh
docker compose -f infra/compose.prod.yml --env-file .env up -d --build
docker compose -f infra/compose.prod.yml --env-file .env ps
curl -k https://127.0.0.1/actuator/health   # may 404 at root; hit via service logs first
docker logs the-chat-auth-service --tail 50
docker logs the-chat-backend-1 --tail 50
```

Verify from outside: `https://api.ankitdev.in/api/v1/auth/...` (after DNS propagates).

### Nightly DB backup cron

```bash
sudo mkdir -p /var/backups/the-chat
sudo crontab -e
# Add:
# 15 2 * * * /opt/the-chat/chat-app-be/infra/scripts/backup-postgres.sh >> /var/log/the-chat-backup.log 2>&1
```

---

## D. GitHub (CI/CD secrets)

Repo → Settings → Secrets and variables → Actions. Add:

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | Lightsail static IP (or `api.ankitdev.in` if DNS ready) |
| `DEPLOY_USER` | `ubuntu` |
| `DEPLOY_SSH_KEY` | Private key contents (matching Lightsail authorized_keys) |
| `DEPLOY_PATH` | `/opt/the-chat` |

Workflow: `.github/workflows/deploy-backend.yml`  
On push to `main` under `chat-app-be/**`, it builds JARs and SCP + `docker compose up -d --build` on the server.

First time: ensure `/opt/the-chat/chat-app-be/.env` and TLS certs already exist on the server (CI does not upload secrets/certs).

---

## E. Vercel (frontend)

1. Import `chat-app-fe` project.
2. Domain: `chat.ankitdev.in`
3. Env:
   ```
   NEXT_PUBLIC_BACKEND_API_URL=https://api.ankitdev.in
   ```
4. Deploy; confirm login sets cookies and WebSocket connects to `wss://api.ankitdev.in/ws`.

---

## F. Monitoring (optional, free)

- [Better Stack](https://betterstack.com) or UptimeRobot: hit  
  - `https://api.ankitdev.in/api/v1/auth/...` health if exposed, or  
  - Docker health via SSH / simple curl to a public health route
- Services expose Spring Actuator `/actuator/health` on their internal ports; you can add an nginx location later if you want a public probe.

---

## G. Smoke-test checklist

- [ ] `https://api.ankitdev.in` responds (TLS OK, Full strict)
- [ ] Register + login from `https://chat.ankitdev.in`
- [ ] Cookies: `Secure`, `SameSite=Lax`, `Domain=.ankitdev.in`
- [ ] Chat WebSocket works (`wss://`)
- [ ] Profile image upload (R2) works
- [ ] Second chat instance receives fan-out (open two browsers / devices)
- [ ] Backup script runs once manually and produces `.sql.gz` files

---

## File map (what the code change added)

| Path | Purpose |
|---|---|
| `infra/compose.prod.yml` | Prod stack: 1 Postgres, Redis, 4 JVMs, nginx |
| `infra/postgres/init-databases.sh` | Creates `auth_db` / `user_db` / `chat_db` |
| `infra/nginx/nginx.prod.conf` | TLS + `ip_hash` + `/ws` |
| `infra/nginx/certs/` | Origin cert mount point (gitignored secrets) |
| `infra/scripts/backup-postgres.sh` | Nightly dumps |
| `.env.production.example` | Env template |
| `.github/workflows/deploy-backend.yml` | Build + SSH deploy |
| Cookie `domain` + Redis `password` | App config for prod |
