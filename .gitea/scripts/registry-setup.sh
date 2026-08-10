#!/usr/bin/env bash
# Make docker use HTTP registry (insecure-registries + /etc/hosts).
# Env: REGISTRY=host:port  REGISTRY_IP=x.x.x.x
set -euo pipefail

REGISTRY="${REGISTRY:?REGISTRY host:port required}"

REGISTRY="${REGISTRY#http://}"
REGISTRY="${REGISTRY#https://}"
HOST="${REGISTRY%%:*}"

merge_insecure() {
  local conf="$1"
  sudo mkdir -p "$(dirname "$conf")"
  if [ -f "$conf" ] && grep -q "\"$REGISTRY\"" "$conf" 2>/dev/null; then
    return 1
  fi
  if command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
    local py
    py="$(command -v python3 || command -v python)"
    sudo "$py" - "$conf" "$REGISTRY" <<'PY'
import json, sys, os
path, reg = sys.argv[1], sys.argv[2]
data = {}
if os.path.isfile(path):
    try:
        with open(path) as f:
            data = json.load(f) or {}
    except Exception:
        data = {}
regs = list(data.get("insecure-registries") or [])
if reg not in regs:
    regs.append(reg)
data["insecure-registries"] = regs
with open(path, "w") as f:
    json.dump(data, f, indent=2)
    f.write("\n")
PY
    return 0
  fi
  if [ ! -f "$conf" ]; then
    echo "{\"insecure-registries\":[\"$REGISTRY\"]}" | sudo tee "$conf" >/dev/null
    return 0
  fi
  echo "Cannot merge $conf (no python). Add insecure-registries: [\"$REGISTRY\"]"
  return 1
}

CHANGED=0
for conf in \
  /etc/docker/daemon.json \
  /var/packages/ContainerManager/etc/dockerd.json \
  /var/packages/Docker/etc/dockerd.json
do
  dir="$(dirname "$conf")"
  if [ "$conf" = /etc/docker/daemon.json ] || [ -d "$dir" ]; then
    if merge_insecure "$conf"; then
      echo "insecure-registries += $REGISTRY in $conf"
      CHANGED=1
    fi
  fi
done

if [ "$CHANGED" = 1 ]; then
  if [ -f /etc/synoinfo.conf ] || [ -d /var/packages/ContainerManager ]; then
    sudo kill -SIGHUP "$(pidof dockerd)" 2>/dev/null || true
  else
    sudo systemctl reload docker 2>/dev/null \
      || sudo systemctl restart docker 2>/dev/null \
      || sudo kill -SIGHUP "$(pidof dockerd)" 2>/dev/null \
      || true
  fi
  sleep 2
fi

echo "registry HTTP ready: $REGISTRY"
