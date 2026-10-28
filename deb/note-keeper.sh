#!/bin/sh
set -e
ROOT=/opt/note-keeper
export LD_LIBRARY_PATH="${ROOT}/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec "${ROOT}/lib/note-keeper" \
  -Djavax.xml.accessExternalDTD=all \
  --spring.config.additional-location=file:${ROOT}/etc/application.yaml \
  "$@"
