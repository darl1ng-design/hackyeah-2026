#!/bin/sh
cd /home/jbr/hub-app || exit 1
docker compose up -d 2>&1 | tail -2
for i in $(seq 1 30); do
  if docker exec hub-app-db-1 pg_isready -U hub >/dev/null 2>&1; then
    echo DB_UP; break
  fi
  sleep 2
done
docker exec hub-app-db-1 psql -U hub -d hub -tAc "select 'bm25_ok', id from innovation order by search_field <@> to_bm25query('toalety lazienki','innovation_bm25_idx') limit 1;"