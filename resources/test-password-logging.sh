#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# add a new user with password 👻
PASSWORD=👻

# docker logs
docker container logs cas >> cas_logs.txt
echo "${PASSWORD}" >> cas_logs.txt
if grep -q "${PASSWORD}" cas_logs.txt; then
  echo "ERROR: Found a non-encrypted password in the docker log file. "
  exit 1
fi

# internal cas logs
docker cp cas:/logs/cas.log /cas_logs
docker cp cas:/logs/cas_audit.log /cas_logs
docker cp cas:/logs/cas_stacktrace.log /cas_logs
if grep -q -R "${PASSWORD}" /logs; then
  echo "ERROR: Found a non-encrypted password in the internal cas log files. "
  exit 1
fi