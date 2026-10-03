-- Model switch: Qwen3-Embedding-4B (2560d) -> Qwen3-Embedding-0.6B (1024d).
-- 0.6B Q8 is ~10x faster on CPU, realtime for the demo.
-- Drops all stored embeddings and resizes the column; SeedRunner re-indexes
-- every innovation on next app start (vector_id reset to null below).
-- Guarded: on a fresh DB Spring AI has not created vector_store yet (Flyway
-- runs before initialize-schema) and there is nothing to re-embed.
do
$$
begin
    if
to_regclass('public.vector_store') is not null then
delete
from vector_store;
alter table vector_store alter column embedding type vector(1024);
end if;
update innovation
set vector_id = null;
end $$;