#!/bin/sh
set -e
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"
exec "$JAVA_BIN" \
  --enable-native-access=ALL-UNNAMED \
  --sun-misc-unsafe-memory-access=allow \
  -Dio.netty.tryReflectionSetAccessible=true \
  -jar /opt/note-keeper/lib/note-keeper.jar \
  --spring.config.location=file:/opt/note-keeper/etc/application.yaml
