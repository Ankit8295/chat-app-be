ALTER TABLE profile_images
    ADD COLUMN original_file_name VARCHAR(255) NOT NULL DEFAULT 'image',
    ADD COLUMN size_bytes BIGINT NOT NULL DEFAULT 0;

ALTER TABLE profile_images
    ALTER COLUMN original_file_name DROP DEFAULT,
    ALTER COLUMN size_bytes DROP DEFAULT;
