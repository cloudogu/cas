#!/usr/bin/env bash
set -euo pipefail
NAME="${NAME:-kc}"
echo "[kc-down] Removing container ${NAME} (if exists)…"
if sudo docker ps -a --format '{{.Names}}' | grep -q "^${NAME}\$"; then
  echo "Container ${NAME} already exists; removing…"
  sudo docker rm -f "${NAME}" >/dev/null
fi
echo "[kc-down] Done."
