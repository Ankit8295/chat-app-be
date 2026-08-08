-- Phase 4: chat_db is a fresh database — no historical app_users baggage.
-- created_by is a soft UUID reference to User service; no cross-DB FK.
CREATE TABLE conversations (
    id          UUID         PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL,
    name        VARCHAR(255),
    about       TEXT,
    image       TEXT,
    created_by  UUID,
    direct_key  VARCHAR(100) UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);
