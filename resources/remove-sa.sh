#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

sourcingExitCode=0
# shellcheck disable=SC1090,SC1091
source "${STARTUP_DIR}"/util.sh || sourcingExitCode=$?
if [[ ${sourcingExitCode} -ne 0 ]]; then
  echo "ERROR: An error occurred while sourcing ${STARTUP_DIR}/util.sh."
fi

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "usage remove-sa.sh account_type [logout_uri] servicename"
  exit 1
fi

TYPE="${1}"
SERVICE="${*: -1}"
if [ "$#" -eq 3 ]; then
  LOGOUT_URI="$2"
fi

if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
  echo "Removing service_accounts/${TYPE}/${SERVICE}/secret key..."
  doguctl config --rm "service_accounts/${TYPE}/${SERVICE}/secret"
elif [ "${TYPE}" == "cas" ]; then
  echo "Removing service_accounts/${TYPE}/${SERVICE}/created key..."
  doguctl config --rm "service_accounts/${TYPE}/${SERVICE}/created"
else
  echo "only the account_types: oidc, oauth, cas are allowed"
  exit 1
fi

if [ -n "${LOGOUT_URI+x}" ]; then
  echo "Removing service_accounts/${TYPE}/${SERVICE}/logout_uri key..."
  doguctl config --rm "service_accounts/${TYPE}/${SERVICE}/logout_uri"
fi

echo "Removing service ${SERVICE} from JSON registry ${SERVICE_REGISTRY}"
FILES=$(ls "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-*.json 2>/dev/null || echo "")

# Check if FILES is empty before counting
if [ -z "$FILES" ]; then
  echo "No files found matching the service ${SERVICE}."
else
  # Count the number of matching files
  FILE_COUNT=$(echo "$FILES" | wc -l)
  echo "Found $FILE_COUNT file(s) matching service ${SERVICE}."
  rm "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-*.json
  echo "Successfully deleted service ${SERVICE}."
fi