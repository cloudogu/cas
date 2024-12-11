#!/bin/bash

# File to track the health status
STATUS_FILE="/tmp/health_status"

# Initialize the file if it doesn't exist
if [ ! -f "$STATUS_FILE" ]; then
  echo "0:$(date +%s)" > "$STATUS_FILE" # Format: failure_count:timestamp
fi

# Read the failure count and timestamp
read -r FAILURE_COUNT LAST_FAILURE_TS < <(cat "$STATUS_FILE")

locale statusCode

if ! doguctl healthy cas; then
  echo "doguctl check failed."
  exit 1
fi

HTTP_STATUS=$(wget -qO- --server-response http://localhost:8080/cas/login 2>&1 | awk '/^  HTTP/{print $2}' | tail -1)

if [ "$HTTP_STATUS" -ne 200 ]; then
  echo "CAS service is not healthy (HTTP status: $HTTP_STATUS)."
  exit 1
fi

echo "CAS service is healthy."
exit 0