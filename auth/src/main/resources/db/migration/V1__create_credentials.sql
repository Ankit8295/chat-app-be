-- Auth service owns credentials: email + password hash.
-- The UUID id is the shared identity key between auth_db and user_db.
CREATE TABLE credentials (
    id           UUID         PRIMARY KEY,
    email        VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_credentials_email ON credentials (email);
