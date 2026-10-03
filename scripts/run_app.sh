#!/bin/sh
cd "$(dirname "$0")/.." || exit 1
rm -f app.log
nohup sh ./backend/mvnw -f backend/pom.xml spring-boot:run > app.log 2>&1 &
echo "APP_PID=$!"