#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

function checkSameVersion() {
  echo "Checking the CAS versions..."
  if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
    echo "FROM and TO versions are the same"
    echo "Set registry flag so startup script can start afterwards..."
    doguctl state "upgrade done"
    echo "Exiting..."
    exit 0
  fi
  echo "Checking the CAS versions... Done!"
}

function removeDeprecatedKeys() {
  echo "Remove deprecated etcd Keys..."
  LDAP_VALUE=$(doguctl config "ldap/use_user_connection_to_fetch_attributes" --default "default")
  if [[ "${LDAP_VALUE}" == "true" ]]; then
    echo
    echo "Note: The method of connecting to LDAP to retrieve user attributes has changed."
    echo "The connection is no longer established via the user connection but via the system connection."
    echo
  fi
  if [[ "${LDAP_VALUE}" != "default" ]]; then
    doguctl config --remove "ldap/use_user_connection_to_fetch_attributes"
  fi

  VALUE=$(doguctl config "logging/translation_messages" --default "default")
  if [[ "${VALUE}" != "default" ]]; then
    doguctl config --remove "logging/translation_messages"
  fi
  echo "Remove deprecated etcd Keys... Done!"
}

function migratePortainerServiceAccount() {
  echo "Migrating portainer service_account..."
  VALUE=$(doguctl config service_accounts/portainer --default "default" || true)
  if [[ "${VALUE}" != "default" ]]; then
      {
        CLIENT_SECRET="$(doguctl config service_accounts/portainer)"
        doguctl config --remove service_accounts/portainer
        doguctl config service_accounts/oauth/portainer "${CLIENT_SECRET}"
      }
  fi
  echo "Migrating portainer service_accounts... Done!"
}

function migrateServiceAccounts() {
  echo "Migrating service accounts..."

  migratePortainerServiceAccount

  if [[ -n "${ECOSYSTEM_MULTINODE+x}" || "${ECOSYSTEM_MULTINODE}" == "false" ]]; then
    migrateServiceAccountsToFolders
    migrateLogoutUrl
  fi

  echo "Migrating service accounts... Done!"
}

function migrateServiceAccountsToFolders() {
  local saTypesToMigrate
  saTypesToMigrate=('oidc' 'oauth')
  migrateServiceAccountsToFoldersByType "${saTypesToMigrate[@]}"
}

function migrateServiceAccountsToFoldersByType() {
  local saType etcdSaUrl requestExitCode errFile outFile
  saType="${1}"
  etcdSaUrl='http://172.17.0.1:4001/v2/keys/config/cas/service_accounts'

  errFile="$(mktemp)"
  outFile="$(mktemp)"
  cleanup() {
    rm "${errFile}" "${outFile}"
  }
  trap cleanup EXIT

  wget -O- "${etcdSaUrl}/${saType}?recursive=false" 1>"${outFile}" 2>"${errFile}" || requestExitCode=$?; true
  if [[ "${requestExitCode}" -eq 8 ]] && grep -q '404 Not Found' "${errFile}"; then
    echo "Service account type '${saType}' not found, skipping..."
    return 0
  elif [[ ! "${requestExitCode}" -eq 0 ]]; then
    echo "Failed to list service accounts of type '${saType}'"
    cat "${errFile}"
    return "${requestExitCode}"
  fi

  jq -r ".node.nodes[] | { service: .key | sub(\".*/${saType}/(?<name>[^/]*)$\";\"\(.name)\"), clientSecretHash: .value } | [.service, .clientSecretHash] | @tsv" < "${outFile}" |
    while IFS=$'\t' read -r service clientSecretHash; do
      echo "Migrate service_account of service '${service}' and type '${saType}'"
      if [[ "${clientSecretHash}" != "default" ]]; then
        {
          doguctl config --remove "service_accounts/${saType}/${service}"
          doguctl config "service_accounts/${saType}/${service}/secret" "${clientSecretHash}"
        }
      fi
    done
}

function migrateLogoutUrl() {
  local etcdDoguUrl etcdDoguResponse
  etcdDoguUrl='http://172.17.0.1:4001/v2/keys/dogu?recursive=true'
  etcdDoguResponse="$(wget -O- "${etcdDoguUrl}")"

  echo "${etcdDoguResponse}" | jq -r '.node.nodes[].nodes | .[] | select(.key | endswith("current")) | { dogu: .key | sub("\/dogu\/(?<name>.*)\/current";"\(.name)"), version: .value } | [.dogu, .version] | @tsv'
  # TODO loop over dogus, get dogu descriptor and search for logoutUrl -> move into folder structure
  # TODO question: should logoutUrl be gotten from folder structure in classic CES as well?
}

##### Functions definition done; Executing post-upgrade now

FROM_VERSION="${1}"
TO_VERSION="${2}"

echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ..."

checkSameVersion
removeDeprecatedKeys
migrateServiceAccounts

echo "Set registry flag so startup script can start afterwards..."
doguctl state "upgrade done"

echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ... Done!"

