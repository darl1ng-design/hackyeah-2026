create table idea_reply (
    id bigserial primary key,
    idea_id bigint not null references idea (id) on delete cascade,
    author_user_id bigint not null references app_user (id),
    body text not null,
    created_at timestamptz not null default now()
);
create index idx_idea_reply_idea_created on idea_reply (idea_id, created_at, id);

create table user_notification (
    id bigserial primary key,
    recipient_user_id bigint not null references app_user (id) on delete cascade,
    idea_id bigint not null references idea (id) on delete cascade,
    kind varchar(20) not null check (kind in ('NEW_IDEA', 'IDEA_REPLY')),
    title varchar(255) not null,
    created_at timestamptz not null default now(),
    read_at timestamptz
);
create index idx_user_notification_recipient_created
    on user_notification (recipient_user_id, created_at desc, id desc);
