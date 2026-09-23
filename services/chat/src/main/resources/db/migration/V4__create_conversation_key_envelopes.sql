CREATE TABLE conversation_key_envelopes (
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL,
    key_version     INT         NOT NULL,
    wrapped_key     TEXT        NOT NULL,
    wrap_nonce      TEXT        NOT NULL,
    eph_public_key  TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (conversation_id, user_id, key_version)
);
