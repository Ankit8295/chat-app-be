-- URL shortener owns short links. user_id is a soft reference to auth/user UUID (no cross-DB FK).
CREATE TABLE urls (
    id          UUID          PRIMARY KEY,
    user_id     UUID,
    long_url    TEXT          NOT NULL,
    short_code  VARCHAR(8)    NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL
);

CREATE UNIQUE INDEX idx_urls_short_code ON urls (short_code);
CREATE INDEX idx_urls_user_id ON urls (user_id);

-- Same destination once per logged-in user; anonymous rows are separate.
CREATE UNIQUE INDEX idx_urls_user_long_url
    ON urls (user_id, long_url)
    WHERE user_id IS NOT NULL;

CREATE UNIQUE INDEX idx_urls_anon_long_url
    ON urls (long_url)
    WHERE user_id IS NULL;
