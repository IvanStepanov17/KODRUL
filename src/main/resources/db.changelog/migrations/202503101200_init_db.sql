create table randomize
(
    id               integer         not null primary key,
    initiatorName    varchar(255),
    loser            varchar(255),
    created_at       timestamp
);