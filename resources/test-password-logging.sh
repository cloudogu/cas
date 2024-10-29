#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# This is a special test that checks if the cas dogu logs unencrypted user passwords

CES_URL=$1
PASSWORD=👻
EXECUTION_TOKEN=""
ADMIN_USER=ces-admin
ADMIN_PW=Ecosystem2016!

# get execution token for login
curl "http://${CES_URL}/cas/login" \
  -v -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure > firstRequest

EXECUTION_TOKEN=$(grep 'name="execution" value=' "firstRequest" | awk '{gsub("value=\"", "", $4); gsub("\"/><input", "", $4); print $4}')
echo "${EXECUTION_TOKEN}"

curl "http://${CES_URL}/cas/login" -X POST \
  -v -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&execution=${EXECUTION_TOKEN}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure

# check docker logs
touch /dogu/cas_logs
sudo docker container logs cas > /dogu/cas_logs
if grep -q "${PASSWORD}" /dogu/cas_logs; then
  echo "ERROR: Found a non-encrypted password in the docker log file. "
  exit 1
fi

# check internal cas logs
mkdir /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas.log /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas_audit.log /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas_stacktrace.log /dogu/cas_internal_logs
touch /dogu/cas_internal_logs/removeme
echo "${PASSWORD}" > /dogu/cas_internal_logs/removeme
if grep -q -R "${PASSWORD}" /dogu/cas_internal_logs; then
  echo "ERROR: Found a non-encrypted password in the internal cas log files. "
  exit 1
fi