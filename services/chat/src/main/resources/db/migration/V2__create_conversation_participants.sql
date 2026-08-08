-- user_id is a soft UUID reference to User service; no cross-DB FK.
CREATE TABLE conversation_participants (
    id              UUID        PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id         UUID        NOT NULL,
    joined_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_conversation_participants UNIQUE (conversation_id, user_id)
);

CREATE INDEX idx_conversation_participants_user_id ON conversation_participants (user_id);
