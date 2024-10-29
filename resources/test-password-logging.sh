#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# add a new user with password 👻
PASSWORD=👻
EXECUTION_TOKEN=
ADMIN_USER=ces-admin
ADMIN_PW=Ecosystem2016!

# get execution token for login
curl 'http://192.168.56.2/cas/login' \
  -v -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure > firstRequest

EXECUTION_TOKEN=$(grep 'name="execution" value=' "firstRequest" | awk '{gsub("value=\"", "", $4); gsub("\"/><input", "", $4); print $4}')

curl 'http://192.168.56.2/cas/login' -X POST \
  -v -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&execution=${EXECUTION_TOKEN}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure

# check docker logs
docker container logs cas >> cas_logs.txt
# remove me later, just for testing
echo "${PASSWORD}" >> cas_logs.txt
if grep -q "${PASSWORD}" cas_logs.txt; then
  echo "ERROR: Found a non-encrypted password in the docker log file. "
  exit 1
fi

# check internal cas logs
docker cp cas:/logs/cas.log /cas_logs
docker cp cas:/logs/cas_audit.log /cas_logs
docker cp cas:/logs/cas_stacktrace.log /cas_logs
if grep -q -R "${PASSWORD}" /logs; then
  echo "ERROR: Found a non-encrypted password in the internal cas log files. "
  exit 1
fi