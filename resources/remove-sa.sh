#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

if [ -z "${1+x}" ] || [ -z "${2+x}" ]; then
  echo "usage remove-sa.sh account_type servicename"
  exit 1
fi

TYPE="${1}"
SERVICE="${2}"

if [ "${TYPE}" != "oidc" ] && [ "${TYPE}" != "oauth" ]; then
  echo "only the account_types: oidc, oauth are allowed"
  exit 1
fi

echo "Removing service_accounts/${TYPE}/${SERVICE} key..."
doguctl config --rm "service_accounts/${TYPE}/${SERVICE}"
