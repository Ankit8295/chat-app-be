ALTER TABLE messages
    ALTER COLUMN content DROP NOT NULL;

ALTER TABLE messages
    ADD COLUMN ciphertext TEXT,
    ADD COLUMN nonce TEXT,
    ADD COLUMN key_version INT;

ALTER TABLE messages
    ADD CONSTRAINT chk_messages_body
        CHECK (content IS NOT NULL OR ciphertext IS NOT NULL);
