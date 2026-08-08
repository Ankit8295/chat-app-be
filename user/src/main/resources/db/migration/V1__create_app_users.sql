-- User service owns: profile data (name, email, image).
-- Credentials (password_hash) live in Auth service (auth_db) since Phase 2.
CREATE TABLE app_users (
    id          UUID         PRIMARY KEY,
    email       VARCHAR(320) NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    image       TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_app_users_email ON app_users (email);
