-- Real ROPS Krakow innovation library entries (public, CC-BY materials) + national services.
-- source_url / video_url are real, clickable links.
alter table innovation add column if not exists source_url varchar(500);

-- BM25 full-text search (Timescale pg_textsearch) over a maintained search field.
create extension if not exists pg_textsearch;

alter table innovation add column if not exists search_field text;

create or replace function innovation_search_field_refresh() returns trigger as $$
begin
    new.search_field := coalesce(new.title,'') || ' ' || coalesce(new.summary,'') || ' '
        || coalesce(new.description,'') || ' ' || coalesce(new.target_group,'');
    return new;
end $$ language plpgsql;

drop trigger if exists innovation_search_field_trg on innovation;
create trigger innovation_search_field_trg before insert or update on innovation
    for each row execute function innovation_search_field_refresh();

update innovation set title = title; -- backfill via trigger

-- 'simple' config: Postgres has no built-in Polish stemmer; BM25 works well token-based.
create index if not exists innovation_bm25_idx on innovation
    using bm25 (search_field) with (text_config='simple');