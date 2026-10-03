#!/bin/sh
# Full-stack bring-up: db + embed + chat + app (Spring Boot in Docker).
cd /home/jbr/hub-app || exit 1
# free host ports held by the old bare-metal app (mvnw spring-boot:run)
for p in $(pgrep -f "spring-boot:run"); do kill "$p" 2>/dev/null; done
for p in $(pgrep -f HubAppApplication); do kill "$p" 2>/dev/null; done
sleep 2
docker compose up -d --build 2>&1 | tail -6
echo COMPOSE_UP
docker compose ps --format '{{.Name}} {{.Status}}'