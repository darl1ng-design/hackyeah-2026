-- V7: Polish-aware BM25. Fixes "shortest doc wins at 100%" bug:
--  * text_config='simple' had no stopwords, so query 'potrzebuje toalety z rampa'
--    matched only the stopword 'z' and BM25 length-norm ranked the shortest doc first.
--  * Adds a Polish stopword dictionary (hub_pl) and ASCII-folds Polish letters in
--    search_field AND in the query path (see InnovationRepository.searchBm25).
-- Every statement is idempotent (Flyway may rerun this after manual application).

-- 0) Drop leftovers from earlier experiments (order matters: config depends on dict).
drop index if exists innovation_bm25_idx;
drop text search configuration if exists public.hub_pl cascade;
drop text search dictionary if exists public.hub_simple cascade;
drop text search dictionary if exists public.hub_unaccent cascade;
drop extension if exists unaccent;

-- 1) search_field now ASCII-folds Polish diacritics (chr() keeps this file pure ASCII).
create or replace function innovation_search_field_refresh() returns trigger as $fn$
begin
    new.search_field := translate(
        coalesce(new.title,'') || ' ' || coalesce(new.summary,'') || ' '
        || coalesce(new.description,'') || ' ' || coalesce(new.target_group,''),
        chr(261)||chr(263)||chr(281)||chr(322)||chr(324)||chr(243)||chr(347)||chr(378)||chr(380)||chr(260)||chr(262)||chr(280)||chr(321)||chr(323)||chr(211)||chr(346)||chr(377)||chr(379),
        'acelnoszzACELNOSZZ');
    return new;
end $fn$ language plpgsql;

update innovation set title = title; -- backfill search_field via trigger

-- 2) Polish stopword dictionary + config (stopwords file ships in the image:
--    docker/polish.stop -> /usr/share/postgresql/18/tsearch_data/polish.stop).
create text search dictionary public.hub_simple (template = simple, stopwords = 'polish');
create text search configuration public.hub_pl (copy = simple);

alter text search configuration public.hub_pl drop mapping for word;
alter text search configuration public.hub_pl drop mapping for asciiword;
alter text search configuration public.hub_pl drop mapping for asciihword;
alter text search configuration public.hub_pl drop mapping for hword;
alter text search configuration public.hub_pl drop mapping for hword_part;

alter text search configuration public.hub_pl add mapping for word     with public.hub_simple;
alter text search configuration public.hub_pl add mapping for asciiword with public.hub_simple;
alter text search configuration public.hub_pl add mapping for asciihword with public.hub_simple;
alter text search configuration public.hub_pl add mapping for hword     with public.hub_simple;
alter text search configuration public.hub_pl add mapping for hword_part with public.hub_simple;

-- 3) Rebuild the BM25 index against hub_pl. text_config MUST be schema-qualified:
--    pg_textsearch fails to resolve unqualified config names during index build.
create index innovation_bm25_idx on innovation
    using bm25 (search_field) with (text_config='public.hub_pl');