CREATE INDEX idx_profile_images_status_created_at
    ON profile_images (status, created_at);
