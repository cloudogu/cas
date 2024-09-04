#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

deleteOldServiceAccounts() {
  local serviceName accountType allTypes typesToDelete
  serviceName="${1}"
  accountType="${2}"

  allTypes=("cas" "oidc" "oauth")
  typesToDelete=( "${allTypes[@]/$accountType}" )
  for type in "${typesToDelete[@]}"; do
    if [ "${type}" == "cas" ]; then
      rmConfigIfExists "service_accounts/${type}/${serviceName}/created"
    else
      rmConfigIfExists "service_accounts/${type}/${serviceName}/secret"
    fi
  done
}

rmConfigIfExists() {
  local key value
  key="${1}"
  value="$(doguctl config --default "DEFAULT" "${key}")"
  if [ "${value}" != "DEFAULT" ]; then
    doguctl config --rm "${key}"
  fi
}

createServiceAccount() {
  {
    if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
      echo "usage create-sa.sh account_type [logout_uri] servicename"
      exit 1
    fi

    TYPE="${1}"
    SERVICE="${*: -1}"
    if [ "$#" -eq 3 ]; then
      LOGOUT_URI="${2}"
    fi

    echo "Create sa for ${SERVICE} with account accountType: ${TYPE}..."

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

    deleteOldServiceAccounts "${SERVICE}" "${TYPE}"

    if [ -n "${LOGOUT_URI+x}" ]; then
        doguctl config "service_accounts/${TYPE}/${SERVICE}/logout_uri" "${LOGOUT_URI}"
    fi

  } >/dev/null 2>&1

  # print client-id so that the service-account can be removed again
  echo "${TYPE}_client_id: ${SERVICE}"

  if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
    # print OAuth credentials for the service
    echo "${TYPE}_client_secret: ${CLIENT_SECRET}"
  fi
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  createServiceAccount "$@"
fi