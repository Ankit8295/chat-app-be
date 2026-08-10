CREATE TABLE profile_images (
    id            UUID         PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    object_key    VARCHAR(512) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_profile_images_user_id ON profile_images (user_id);
CREATE INDEX idx_profile_images_user_status ON profile_images (user_id, status);
