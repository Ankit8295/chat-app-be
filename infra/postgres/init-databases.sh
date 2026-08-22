#!/bin/bash
# Runs once on first Postgres volume init. Creates the three service databases + users.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER ${AUTH_DB_USER} WITH PASSWORD '${AUTH_DB_PASSWORD}';
    CREATE DATABASE auth_db OWNER ${AUTH_DB_USER};
    GRANT ALL PRIVILEGES ON DATABASE auth_db TO ${AUTH_DB_USER};

    CREATE USER ${USER_DB_USER} WITH PASSWORD '${USER_DB_PASSWORD}';
    CREATE DATABASE user_db OWNER ${USER_DB_USER};
    GRANT ALL PRIVILEGES ON DATABASE user_db TO ${USER_DB_USER};

    CREATE USER ${CHAT_DB_USER} WITH PASSWORD '${CHAT_DB_PASSWORD}';
    CREATE DATABASE chat_db OWNER ${CHAT_DB_USER};
    GRANT ALL PRIVILEGES ON DATABASE chat_db TO ${CHAT_DB_USER};
EOSQL

# Postgres 15+ requires schema privileges on the new DBs for non-superuser owners to run Flyway.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "auth_db" <<-EOSQL
    GRANT ALL ON SCHEMA public TO ${AUTH_DB_USER};
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "user_db" <<-EOSQL
    GRANT ALL ON SCHEMA public TO ${USER_DB_USER};
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "chat_db" <<-EOSQL
    GRANT ALL ON SCHEMA public TO ${CHAT_DB_USER};
EOSQL
