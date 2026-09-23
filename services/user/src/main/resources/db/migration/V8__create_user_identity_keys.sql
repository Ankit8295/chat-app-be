CREATE TABLE user_identity_keys (
    user_id              UUID         PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    public_key           TEXT         NOT NULL,
    wrapped_private_key  TEXT         NOT NULL,
    wrap_nonce           TEXT         NOT NULL,
    kdf_salt             TEXT         NOT NULL,
    kdf_iterations       INT          NOT NULL,
    algorithm            VARCHAR(64)  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);
