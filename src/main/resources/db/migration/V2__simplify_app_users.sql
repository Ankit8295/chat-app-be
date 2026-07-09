ALTER TABLE app_users
    RENAME COLUMN display_name TO name;

ALTER TABLE app_users
    DROP COLUMN role;
