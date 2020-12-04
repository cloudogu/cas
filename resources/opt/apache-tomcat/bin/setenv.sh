#!/bin/sh
JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"
JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=/etc/ssl/truststore.jks"
JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStorePassword=changeit"
if [ "$(doguctl config "container_config/memory_limit" -d "empty")" != "empty" ];  then
  JAVA_OPTS="$JAVA_OPTS -XX:MaxRAMPercentage=85.0"
  JAVA_OPTS="$JAVA_OPTS -XX:MinRAMPercentage=50.0"
fi