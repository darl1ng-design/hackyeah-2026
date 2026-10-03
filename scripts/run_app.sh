#!/bin/sh
cd /home/jbr/hub-app || exit 1
rm -f /home/jbr/hub-app/app.log
nohup sh ./mvnw spring-boot:run > /home/jbr/hub-app/app.log 2>&1 &
echo "APP_PID=$!"