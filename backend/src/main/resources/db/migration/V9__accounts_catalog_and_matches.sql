create table app_user
(
    id            bigserial primary key,
    email         varchar(320) not null unique,
    password_hash varchar(255) not null,
    display_name  varchar(255) not null,
    role          varchar(20) not null check (role in ('MEMBER', 'STAFF', 'ADMIN')),
    created_at    timestamptz not null default now()
);

alter table innovation add column created_at timestamptz not null default now();
update innovation set status = 'ROZWOJ'
where status is null or status not in ('ROZWOJ', 'TESTOWANA', 'WDROZONA');
alter table innovation alter column status set not null;
alter table innovation add constraint chk_innovation_status
    check (status in ('ROZWOJ', 'TESTOWANA', 'WDROZONA'));

update innovation set region = 'MALOPOLSKA'
where lower(trim(region)) in ('małopolska', 'malopolska', 'malopolskie');
update innovation set region = upper(trim(region)) where region is not null;
update innovation set region = 'INNE'
where region is not null and region not in (
    'DOLNOSLASKIE', 'KUJAWSKO_POMORSKIE', 'LUBELSKIE', 'LUBUSKIE',
    'LODZKIE', 'MALOPOLSKA', 'MAZOWIECKIE', 'OPOLSKIE', 'PODKARPACKIE',
    'PODLASKIE', 'POMORSKIE', 'SLASKIE', 'SWIETOKRZYSKIE',
    'WARMINSKO_MAZURSKIE', 'WIELKOPOLSKIE', 'ZACHODNIOPOMORSKIE',
    'POLSKA', 'INNE');

update problem_report set status = 'NOWE'
where status not in ('NOWE', 'PRZYPISANE', 'W_REALIZACJI', 'ZAMKNIETE');
alter table problem_report add constraint chk_report_status
    check (status in ('NOWE', 'PRZYPISANE', 'W_REALIZACJI', 'ZAMKNIETE'));
update problem_report set region = 'MALOPOLSKA'
where lower(trim(region)) in ('małopolska', 'malopolska', 'malopolskie');
update problem_report set region = upper(trim(region)) where region is not null;
update problem_report set region = 'INNE'
where region is not null and region not in (
    'DOLNOSLASKIE', 'KUJAWSKO_POMORSKIE', 'LUBELSKIE', 'LUBUSKIE',
    'LODZKIE', 'MALOPOLSKA', 'MAZOWIECKIE', 'OPOLSKIE', 'PODKARPACKIE',
    'PODLASKIE', 'POMORSKIE', 'SLASKIE', 'SWIETOKRZYSKIE',
    'WARMINSKO_MAZURSKIE', 'WIELKOPOLSKIE', 'ZACHODNIOPOMORSKIE',
    'POLSKA', 'INNE');

alter table idea add column author varchar(255) not null default 'Anonim';
alter table idea add column owner_user_id bigint references app_user (id);
alter table idea add column moderation_status varchar(20) not null default 'APPROVED';
alter table idea alter column moderation_status set default 'PENDING';
update idea set stage = 'MYSL'
where stage is null or stage not in ('MYSL', 'PROTOTYP', 'TESTY', 'WDROZENIE');
alter table idea alter column stage set not null;
alter table idea add constraint chk_idea_stage
    check (stage in ('MYSL', 'PROTOTYP', 'TESTY', 'WDROZENIE'));
alter table idea add constraint chk_idea_moderation
    check (moderation_status in ('PENDING', 'APPROVED', 'REJECTED'));
create index idx_idea_owner_created on idea (owner_user_id, created_at desc);
create index idx_idea_moderation_created on idea (moderation_status, created_at desc);

update resource set kind = 'PUBLIKACJE'
where kind not in ('BIBLIOTEKA', 'RAPORTY', 'STATYSTYKI', 'MAPA', 'PUBLIKACJE', 'CANVAS');
alter table resource add constraint chk_resource_kind
    check (kind in ('BIBLIOTEKA', 'RAPORTY', 'STATYSTYKI', 'MAPA', 'PUBLIKACJE', 'CANVAS'));

create table report_match
(
    id            bigserial primary key,
    report_id     bigint not null references problem_report (id) on delete cascade,
    innovation_id bigint not null references innovation (id),
    rank_order    integer not null,
    why           text not null,
    similarity    double precision not null,
    unique (report_id, rank_order)
);
create index idx_report_match_report on report_match (report_id, rank_order);
