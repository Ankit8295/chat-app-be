CREATE TABLE friendships (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    friend_user_id UUID        NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_friendships_user_friend UNIQUE (user_id, friend_user_id)
);

CREATE INDEX idx_friendships_user_id ON friendships (user_id);
