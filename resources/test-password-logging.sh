#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail
set -e

# This is a special test script that checks if the cas dogu logs unencrypted user passwords
# It checks the internal cas logs and the external docker logs
echo "Testing CAS logs for unencrypted passwords"

CES_URL=$1
EXECUTION_TOKEN=""
# admin user and password from integration tests
ADMIN_USER=ces-admin
ADMIN_PW=Ecosystem2016!

echo "Logging in to CAS"
# get execution token for login
curl "http://${CES_URL}/cas/login" \
  -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure > firstRequest
EXECUTION_TOKEN=$(grep 'name="execution" value=' "firstRequest" | awk '{gsub("value=\"", "", $4); gsub("\"/><input", "", $4); print $4}')
# login request that gets logged by cas
curl "http://${CES_URL}/cas/login" -X POST \
  -L --data-raw "username=${ADMIN_USER}&${ADMIN_PW}&execution=${EXECUTION_TOKEN}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure

# check docker logs
echo "Checking external cas docker logs for unencrypted passwords"
touch /dogu/cas_logs
sudo docker container logs cas > /dogu/cas_logs
if grep -q "${ADMIN_PW}" /dogu/cas_logs; then
  echo "ERROR: Found a non-encrypted password in the docker log file. Exiting the pipeline..."
  exit 1
fi

# check internal cas logs
echo "Checking internal cas logs for unencrypted passwords"
mkdir /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas.log /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas_audit.log /dogu/cas_internal_logs
sudo docker cp cas:/logs/cas_stacktrace.log /dogu/cas_internal_logs
if grep -q -R "${ADMIN_PW}" /dogu/cas_internal_logs; then
  echo "ERROR: Found a non-encrypted password in the internal cas log files. Exiting the pipeline..."
  exit 1
fi

echo "Found no unencrypted passwords in the cas logs"