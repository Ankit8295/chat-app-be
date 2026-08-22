#!/usr/bin/env bash
# Nightly Postgres dump of auth_db, user_db, chat_db → gzip → optional upload to R2.
# Install on the Lightsail box as a cron job (see docs/DEPLOY.md).
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/the-chat}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
COMPOSE_FILE="${COMPOSE_FILE:-/opt/the-chat/chat-app-be/infra/compose.prod.yml}"
ENV_FILE="${ENV_FILE:-/opt/the-chat/chat-app-be/.env}"

mkdir -p "$BACKUP_DIR"

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

for DB in auth_db user_db chat_db; do
  OUT="${BACKUP_DIR}/${DB}_${TIMESTAMP}.sql.gz"
  echo "Dumping ${DB} -> ${OUT}"
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T postgres \
    pg_dump -U "${POSTGRES_SUPERUSER:-postgres}" "$DB" | gzip > "$OUT"
done

# Optional: upload to R2 if BACKUP_R2_* vars are set (requires aws CLI configured for R2)
if [[ -n "${BACKUP_R2_BUCKET:-}" && -n "${BACKUP_R2_ENDPOINT:-}" ]]; then
  export AWS_ACCESS_KEY_ID="${BACKUP_R2_ACCESS_KEY_ID}"
  export AWS_SECRET_ACCESS_KEY="${BACKUP_R2_SECRET_ACCESS_KEY}"
  for f in "${BACKUP_DIR}"/*_"${TIMESTAMP}".sql.gz; do
    aws s3 cp "$f" "s3://${BACKUP_R2_BUCKET}/postgres/$(basename "$f")" \
      --endpoint-url "${BACKUP_R2_ENDPOINT}"
  done
fi

find "$BACKUP_DIR" -name '*.sql.gz' -mtime +"${RETENTION_DAYS}" -delete
echo "Backup complete at ${TIMESTAMP}"
