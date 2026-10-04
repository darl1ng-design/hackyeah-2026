create table hub_workflow_record (
    id bigserial primary key,
    module varchar(32) not null check (module in (
        'TESTER_FEEDBACK', 'GRANT_CALL', 'GRANT_APPLICATION', 'MENTOR_CONVERSATION', 'MENTOR_MESSAGE', 'MIDDLEMAN_PLAN'
    )),
    owner_user_id bigint not null references app_user (id) on delete cascade,
    reference_id bigint,
    status varchar(32) not null,
    title varchar(255) not null,
    payload text not null,
    dedupe_key varchar(255) unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index idx_hub_workflow_owner_module_created
    on hub_workflow_record (owner_user_id, module, created_at desc, id desc);
create index idx_hub_workflow_module_status_created
    on hub_workflow_record (module, status, created_at desc, id desc);
create index idx_hub_workflow_reference
    on hub_workflow_record (module, reference_id);
