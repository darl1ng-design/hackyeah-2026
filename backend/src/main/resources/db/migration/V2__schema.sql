create table challenge_area
(
    id          bigserial primary key,
    name        varchar(255) not null unique,
    description varchar(1000)
);

create table innovation
(
    id           bigserial primary key,
    title        varchar(255) not null,
    summary      varchar(1000),
    description  text,
    target_group varchar(255),
    status       varchar(50),
    region       varchar(255),
    video_url    varchar(500),
    area_id      bigint references challenge_area (id),
    vector_id    varchar(255)
);

create table problem_report
(
    id          bigserial primary key,
    description text        not null,
    region      varchar(255),
    author_name varchar(255),
    area_id     bigint references challenge_area (id),
    status      varchar(50) not null default 'NOWE',
    created_at  timestamptz not null default now()
);

create table idea
(
    id           bigserial primary key,
    title        varchar(255) not null,
    essence      text,
    target_group varchar(255),
    stage        varchar(50),
    description  text,
    created_at   timestamptz  not null default now()
);

create index idx_innovation_area on innovation (area_id);
create index idx_problem_report_area on problem_report (area_id);