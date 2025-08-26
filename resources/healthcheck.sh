#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# Script to perform a health check and manage container restarts based on failures.
#
# This script monitors the health of the CAS-Dogu. It uses `doguctl`
# and an HTTP endpoint to perform the health checks. If the dogu is determined to be unhealthy,
# the script either triggers a container restart or records the failure status based on the provided timeout.
#
# Usage:
#   ./health_check.sh [TIMEOUT] [STATUS_FILE]
#
# Parameters:
#   TIMEOUT (optional): The time (in seconds) after which the container should be forcibly killed
#                       and restarted if the dogu remains unhealthy. A value of 0 disables restarts.
#                       Default: 0 (disabled).
#   STATUS_FILE (optional): The file used to track failure counts and timestamps. This file is used
#                           to determine whether the timeout threshold has been exceeded.
#                           Default: /tmp/health_status
#
# Notes:
# - Ensure that the script has execute permissions (`chmod +x health_check.sh`).
# - Configure the script in the Docker container as a health check using Docker's `HEALTHCHECK` instruction.
#
# Example:
#   ./health_check.sh 300 /tmp/health_status
#   This command sets a 5-minute timeout and tracks failure counts using the `/tmp/health_status` file.


# Default timeout value (0 disables the timeout)
DEFAULT_TIMEOUT=0
DEFAULT_STATUS_FILE="/tmp/health_status"

TIMEOUT=${1:-$DEFAULT_TIMEOUT}
STATUS_FILE=${2:-$DEFAULT_STATUS_FILE}

HEALTH_STATUS=0

# Writes the health status to a specified status file. The status is represented as a combination of
# failure count and a timestamp, separated by a colon.
#
# Arguments:
#   $1 - The failure count (e.g., number of consecutive health check failures).
#   $2 - The timestamp (e.g., time of the last failure or reset).
#
# Example:
#   writeHealthStatus 3 1690000000
#   # This writes "3:1690000000" to the file defined in STATUS_FILE.
function writeHealthStatus() {
  local failure_count=$1   # First argument: failure count
  local timestamp=$2       # Second argument: timestamp

  echo "${failure_count}:${timestamp}" > "$STATUS_FILE" # Update the file with new values
}

# Terminates the running Apache Tomcat process forcefully.
function killApacheTomcat() {
  local tomcatPID=$(pgrep -f apache-tomcat)
  kill -9 $tomcatPID
}

# Initialize the health status file if it doesn't exist
if [[ ! -f "$STATUS_FILE" ]]; then
  echo "health status file does not exist, create it"
  writeHealthStatus "0" "$(date +%s)"
fi

# Read the failure count and timestamp
IFS=":" read -r FAILURE_COUNT LAST_FAILURE_TS < "$STATUS_FILE"

# Perform health check using doguctl
if ! doguctl healthy -t 10 cas; then
  echo "doguctl is unhealthy"
  HEALTH_STATUS=1
fi

# Perform health check against cas endpoint
if [[ "$HEALTH_STATUS" -eq 0 ]]; then
  HTTP_STATUS=$(wget --spider -S --tries=1 --timeout=10 "http://localhost:8080/cas/actuator/health" 2>&1 | awk '/^  HTTP/{print $2}' | tail -1) || HTTP_STATUS=0

  if [[ "$HTTP_STATUS" -ne 200 ]]; then
    echo "cas health endpoint is unhealthy"
    HEALTH_STATUS=1
  fi
  HEALTH_STATUS=0
fi

CURRENT_TS=$(date +%s)

# Check general health status
if [[ "$HEALTH_STATUS" -eq 0 ]]; then
  writeHealthStatus "0" "$CURRENT_TS"
  exit 0
fi

# Increase failure count
FAILURE_COUNT=$((FAILURE_COUNT + 1))

if [[ "$FAILURE_COUNT" -eq "1" ]]; then
  writeHealthStatus "$FAILURE_COUNT" "$CURRENT_TS"
  exit 1
else
  writeHealthStatus "$FAILURE_COUNT" "$LAST_FAILURE_TS"
fi

# Exit in case restart is disabled
if [[ "$TIMEOUT" -eq 0 ]]; then
  exit 1
fi


echo "begin check for restart"

# Calculate the time elapsed since the first failure
TIME_ELAPSED=$((CURRENT_TS - LAST_FAILURE_TS))

echo "Time elapsed after first failure: ${TIME_ELAPSED}s with timeout of ${TIMEOUT}s"

# Check if the timeout has been exceeded
if [[ "$TIME_ELAPSED" -ge "$TIMEOUT" ]]; then
  writeHealthStatus "0" "$CURRENT_TS" # Reset Counter
  killApacheTomcat
fi

exit 1