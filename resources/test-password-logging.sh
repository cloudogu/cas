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
ADMIN_USER=admin
ADMIN_PW=admin
PWD_LOGGING_USER=pwdlogging
PWD_LOGGING_PASSWORD=👻

# change cas warn level
sudo docker container exec cas doguctl config logging/root DEBUG
sudo docker container restart cas
# wait for cas to be ready
curl -L -v https://192.168.56.2/cas/login --insecure
until [ "`docker container exec cas doguctl state`"=="ready" ]; do
    echo "waiting for CAS to be ready"
    sleep 1;
done;
sleep 200

echo "Creating new testuser ${PWD_LOGGING_USER} with password ${PWD_LOGGING_PASSWORD}"
# create new user
POST_DATA=$(jq -n --arg name "${PWD_LOGGING_USER}" --arg pw "${PWD_LOGGING_PASSWORD}" \
'{displayName: $name, givenname: $name, mail: "adminpwdlogging@admin.org", surname: $name, username: $name, password: $pw, pwdReset: false, external :false, memberOf: ["administrators"]}'
)
curl -v --insecure -u "${ADMIN_USER}:${ADMIN_PW}" "https://${CES_URL}/usermgt/api/users" -H "Content-Type: application/json; charset=UTF-8" \
--data-raw "${POST_DATA}" -L

echo "Logging in to CAS with new user"
# get execution token for login
curl "https://${CES_URL}/cas/login" \
  -L --insecure -v > firstRequest
EXECUTION_TOKEN=$(grep 'name="execution" value=' "firstRequest" -m 1 | awk '{gsub("value=\"", "", $4); gsub("\"/><input", "", $4); print $4}')
echo "${EXECUTION_TOKEN}"
# this request is authorized
curl "https://${CES_URL}/cas/login" \
  -v --data-raw "username=${PWD_LOGGING_USER}&password=${PWD_LOGGING_PASSWORD}&execution=${EXECUTION_TOKEN}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure
# this request is unauthorized, but logs will still appear in the cas
curl "https://${CES_URL}/cas/login" \
  -v --data-raw "username=wrongUser&password=${PWD_LOGGING_PASSWORD}&execution=${EXECUTION_TOKEN}&_eventId=submit&geolocation=&deviceFingerprint=" \
  --insecure

echo "Creating valid service ticket with new user"
# this valid service ticket will appear in the cas logs as well
curl  -L "https://${CES_URL}/cas/v1/tickets" --data "username=${PWD_LOGGING_USER}&password=${PWD_LOGGING_PASSWORD}" --insecure \
 -H 'Content-type: Application/x-www-form-urlencoded' --http1.0 -X POST > serviceTicket
ticketGrantingTicket=$(grep -Po TGT-.*cas serviceTicket)
curl -L "https://${CES_URL}/cas/v1/tickets/${ticketGrantingTicket}?service=https%3A%2F%2F192.168.56.2%2Fcas/login" --insecure \
 -H 'Content-type: Application/x-www-form-urlencoded' --http1.0 -X POST --data "username=${PWD_LOGGING_USER}&password=${PWD_LOGGING_PASSWORD}" > serviceTicket
validTicket=$(cat serviceTicket)
curl -L -X GET --insecure "https://${CES_URL}/cas/p3/serviceValidate?service=https://${CES_URL}/cas/login&ticket=${validTicket}" --http1.0 > serviceTicket

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