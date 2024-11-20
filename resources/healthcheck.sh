#!/bin/bash

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

function clearStatusFile() {
  if [[ -f "$STATUS_FILE" ]]; then
    rm ${STATUS_FILE}
  fi
}

# Perform health check using doguctl
if ! doguctl healthy -t 10 cas; then
  echo "doguctl is unhealthy"
  HEALTH_STATUS=1
fi

# Perform health check against cas endpoint
if [[ "$HEALTH_STATUS" -eq 0 ]]; then
  HTTP_STATUS=$(wget --spider -S --tries=1 --timeout=10 "http://localhost:8080/cas/actuator/health" 2>&1 | awk '/^  HTTP/{print $2}' | tail -1)

  if [[ -z "$HTTP_STATUS" ]] || [[ "$HTTP_STATUS" -ne 200 ]]; then
    echo "cas is unhealthy"
    HEALTH_STATUS=1
  fi
fi

# Check general health status
if [[ "$HEALTH_STATUS" -eq 0 ]]; then
  clearStatusFile
  exit 0
fi

# Exit in case restart is disabled
if [[ "$TIMEOUT" -eq 0 ]]; then
  exit 1
fi

echo "begin check for restart"

# Begin check for restart container
LAST_FAILURE_TS=$(date +%s)

# Initialize the file if it doesn't exist
if [[ ! -f "$STATUS_FILE" ]]; then
  echo "status file does not exists, create it"
  echo "1:$LAST_FAILURE_TS" > "$STATUS_FILE" # Format: failure_count:timestamp
  exit 1
fi

# Read the failure count and timestamp
IFS=":" read -r FAILURE_COUNT LAST_FAILURE_TS < "$STATUS_FILE"

echo "read status file with ${FAILURE_COUNT} and ${LAST_FAILURE_TS}"

# Calculate the time elapsed since the first failure
CURRENT_TS=$(date +%s)
TIME_ELAPSED=$((CURRENT_TS - LAST_FAILURE_TS))

echo "time elapsed: ${TIME_ELAPSED}"

# Check if the timeout has been exceeded
if [[ "$TIME_ELAPSED" -ge "$TIMEOUT" ]]; then
  clearStatusFile
  kill -s 9 -1
fi

# Increase failure count
FAILURE_COUNT=$((FAILURE_COUNT + 1))

# Save the updated failure count and timestamp
echo "$FAILURE_COUNT:$LAST_FAILURE_TS" > "$STATUS_FILE"
exit 1