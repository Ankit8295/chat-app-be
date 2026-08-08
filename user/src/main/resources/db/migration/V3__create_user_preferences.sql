-- last_conversation_id is a soft UUID reference — no FK to chat_db.conversations.
-- Cross-DB foreign keys are not allowed in database-per-service architecture.
CREATE TABLE user_preferences (
    user_id              UUID PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    last_conversation_id UUID,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL
);
