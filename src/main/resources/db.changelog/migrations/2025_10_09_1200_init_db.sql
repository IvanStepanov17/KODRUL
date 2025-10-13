CREATE TABLE IF NOT EXISTS telegram_users (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    user_name VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    is_bot BOOLEAN DEFAULT FALSE,
    last_seen TIMESTAMPTZ NOT NULL,
    first_seen TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    chat_id BIGINT NOT NULL,
    chat_title VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    added_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_group FOREIGN KEY (group_id) REFERENCES chat_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES telegram_users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS scheduled_posts (
     id BIGSERIAL PRIMARY KEY,
     chat_id BIGINT NOT NULL,
     group_name VARCHAR(255) NOT NULL,
     cron_expression VARCHAR(50) NOT NULL,
     description VARCHAR(255),
     message_text VARCHAR(1000),
     image_url VARCHAR(255),
     is_active BOOLEAN NOT NULL DEFAULT TRUE,
     last_sent TIMESTAMPTZ,
     created_at TIMESTAMPTZ NOT NULL,
     created_by BIGINT NOT NULL
);