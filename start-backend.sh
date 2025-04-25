#!/bin/bash
source /etc/default/ledance-backend
exec /usr/bin/java \
  -Dspring.config.additional-location=classpath:/application.properties \
  -jar "$LEDANCE_HOME/backend/target/backend-1.0.jar"
