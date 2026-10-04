alter table innovation add column published boolean not null default true;
alter table resource add column published boolean not null default true;
create index idx_innovation_published_created on innovation (published, created_at desc);
create index idx_resource_published on resource (published);
