-- sender_id is a soft UUID reference to User service; no cross-DB FK.
CREATE TABLE messages (
    id              UUID        PRIMARY KEY,
    conversation_id UUID        NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id       UUID        NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_messages_conversation_created_at_id
    ON messages (conversation_id, created_at DESC, id DESC);
