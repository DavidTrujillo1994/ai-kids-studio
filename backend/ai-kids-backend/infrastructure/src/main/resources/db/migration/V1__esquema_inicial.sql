create table series (
    id          text primary key,
    name        text not null,
    description text not null,
    created_at  integer not null,
    updated_at  integer not null
);

create table story_character (
    id             text primary key,
    series_id      text not null references series (id),
    asset_key      text not null,
    role           text not null,
    name           text not null,
    personality    text not null,
    design_version integer not null,
    design_status  text not null,
    unique (series_id, asset_key)
);

create table episode (
    id          text primary key,
    series_id   text not null references series (id),
    folder      text not null unique,
    title       text not null,
    idea        text not null,
    environment text not null,
    script      text not null,
    status      text not null,
    created_at  integer not null,
    updated_at  integer not null
);
create index episode_series on episode (series_id, created_at);

create table review (
    id         integer primary key autoincrement,
    episode_id text not null references episode (id) on delete cascade,
    position   integer not null,
    decision   text not null,
    comments   text not null,
    at         integer not null
);
create index review_episode on review (episode_id);

create table production_job (
    id           text primary key,
    episode_id   text not null references episode (id) on delete cascade,
    status       text not null,
    stage        text not null,
    progress     integer not null,
    message      text not null,
    error_detail text,
    created_at   integer not null,
    started_at   integer,
    finished_at  integer
);
create index job_episode on production_job (episode_id, created_at);
create index job_status on production_job (status);
