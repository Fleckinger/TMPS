create table usr
(
    id                 uuid not null,
    telegram_bot_token varchar(64),
    channel_id         varchar(64),
    username           varchar(64),
    time_zone          timestamp,
    primary key (id)
);

create table post
(
    id        uuid not null,
    text      varchar(4096),
    post_date timestamp,
    is_posted boolean,
    primary key (id)
);

create table media
(
    id       uuid not null,
    type     varchar(64),
    field_id varchar(64),
    index    smallint,
    primary key (id)
);

