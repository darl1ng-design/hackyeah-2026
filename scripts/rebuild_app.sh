#!/bin/sh
# Rebuild + restart only the app service (db/embed/chat keep running).
cd /home/jbr/hub-app || exit 1
docker compose build app 2>&1 | tail -2
docker compose up -d app 2>&1 | tail -2
for i in $(seq 1 60); do
  code=$(curl -s -m 2 -o /dev/null -w '%{http_code}' http://localhost:${APP_PORT:-8083}/login)
  if [ "$code" = "200" ]; then echo APP_READY; break; fi
  sleep 3
done