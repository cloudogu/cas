#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

{
  if [ -z "${1+x}" ] || [ -z "${2+x}" ]; then
    echo "usage create-sa.sh account_type servicename"
    exit 1
  fi

  TYPE="${1}"
  SERVICE="${2}"

  echo "Create sa for ${SERVICE} with account type: ${TYPE}..."

  if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
    CLIENT_SECRET=$(doguctl random -l 16)
    CLIENT_SECRET_HASH=$(echo -n "${CLIENT_SECRET}" | sha256sum | awk '{print $1}')

    doguctl config "service_accounts/${TYPE}/${SERVICE}/secret" "${CLIENT_SECRET_HASH}"
  elif [ "${TYPE}" == "cas" ]; then
    # Set value `created` because doguctl requires a value to be set
    doguctl config "service_accounts/${TYPE}/${SERVICE}/created" "true"
  else
    echo "only the account_types: oidc, oauth, cas are allowed"
    exit 1
  fi

  if [ -n "${3+x}" ]; then
      doguctl config "service_accounts/${TYPE}/${SERVICE}/logout_uri" "${3}"
  fi

} >/dev/null 2>&1

if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
  # print OAuth credentials for the service
  echo "${TYPE}_client_id: ${SERVICE}"
  echo "${TYPE}_client_secret: ${CLIENT_SECRET}"
fi
