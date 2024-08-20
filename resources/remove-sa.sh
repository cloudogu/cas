#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
  echo "usage create-sa.sh account_type [logout_uri] servicename"
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