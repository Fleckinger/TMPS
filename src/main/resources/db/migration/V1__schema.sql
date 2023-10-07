CREATE TABLE IF NOT EXISTS usr
(
    id               UUID NOT NULL UNIQUE,
    telegram_user_id BIGINT UNIQUE,
    channel_id       BIGINT,
    username         VARCHAR(64),
    time_zone        VARCHAR(64),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS post
(
    id             UUID NOT NULL UNIQUE,
    user_id        UUID NOT NULL,
    media_group_id VARCHAR(64),
    text           VARCHAR(4096),
    post_date      TIMESTAMP,
    is_posted      BOOLEAN,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS media
(
    id       BIGSERIAL NOT NULL UNIQUE,
    post_id  UUID NOT NULL,
    type     VARCHAR(64),
    file_id  VARCHAR(128),
    index    SMALLINT,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS post
    ADD CONSTRAINT fk_post_user
        FOREIGN KEY (user_id)
            REFERENCES usr;

ALTER TABLE IF EXISTS media
    ADD CONSTRAINT fk_media_post
        FOREIGN KEY (post_id)
            REFERENCES post;