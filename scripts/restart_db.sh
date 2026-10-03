#!/bin/sh
cd /home/jbr/hub-app || exit 1
docker compose down -v 2>&1 | tail -1
docker compose up -d 2>&1 | tail -1
sleep 8
docker compose exec -T db psql -U hub -d hub -c "select extname from pg_extension;" 2>&1
echo DB_READY