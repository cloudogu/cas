#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

checkSameVersion() {
  echo "Checking the CAS versions..."
  if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
    echo "FROM and TO versions are the same"
    echo "Set registry flag so startup script can start afterwards..."
    doguctl state "upgrade done"
    doguctl config --rm "local_state"
    echo "Exiting..."
    exit 0
  fi
  echo "Checking the CAS versions... Done!"
}

removeDeprecatedKeys() {
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

getEtcdEndpoint() {
  tr -d '[:space:]' < "${NODE_MASTER_FILE}"
}

migratePortainerServiceAccount() {
  echo "Migrating portainer service account..."
  VALUE=$(doguctl config service_accounts/portainer --default "default" || true)
  if [[ "${VALUE}" != "default" ]]; then
      {
        CLIENT_SECRET="$(doguctl config service_accounts/portainer)"
        doguctl config --remove service_accounts/portainer
        doguctl config service_accounts/oauth/portainer "${CLIENT_SECRET}"
        echo "Migrating portainer service account... Done!"
      }
  else
    echo "Migrating portainer service account... Nothing to do!"
  fi
}

migrateServiceAccountsToFoldersByType() {
  local saType etcdSaUrl errFile outFile requestExitCode
  saType="${1}"
  etcdSaUrl="http://$(getEtcdEndpoint):4001/v2/keys/config/cas/service_accounts"

  echo "Migrating service accounts of type '${saType}'..."

  errFile="$(mktemp)"
  outFile="$(mktemp)"
  # shellcheck disable=SC2064
  trap "rm ${errFile} ${outFile}" EXIT

  set +o errexit # temporarily disable immediate exit on error to handle not found
  wget -O- "${etcdSaUrl}/${saType}?recursive=false" 1>"${outFile}" 2>"${errFile}"
  requestExitCode=$?
  set -o errexit

  if [[ "${requestExitCode}" -eq 8 ]] && grep -q '404 Not Found' "${errFile}"; then
    echo "Service account type '${saType}' not found, skipping..."
    return 0
  elif [[ ! "${requestExitCode}" -eq 0 ]]; then
    echo "Failed to list service accounts of type '${saType}'"
    cat "${errFile}"
    exit "${requestExitCode}"
  fi

  jq -r ".node.nodes[] | select(.dir | not) | { service: .key | sub(\".*/${saType}/(?<name>[^/]*)$\";\"\(.name)\"), clientSecretHash: .value } | [.service, .clientSecretHash] | @tsv" < "${outFile}" |
    while IFS=$'\t' read -r service clientSecretHash; do
      echo "Migrating service account directory for '${service}'"
      doguctl config --remove "service_accounts/${saType}/${service}"
      doguctl config "service_accounts/${saType}/${service}/secret" "${clientSecretHash}"
    done

  echo "Migrating service accounts of type '${saType}'... Done!"
}

migrateServiceAccountsToFolders() {
  migrateServiceAccountsToFoldersByType 'oidc'
  migrateServiceAccountsToFoldersByType 'oauth'
}

migrateLogoutUri() {
  echo "Migrating logout URIs..."

  local etcdDoguUrl etcdDoguResponse dogu version
  etcdDoguUrl="http://$(getEtcdEndpoint):4001/v2/keys/dogu?recursive=true"
  etcdDoguResponse="$(wget -O- "${etcdDoguUrl}")"

  echo "${etcdDoguResponse}" | jq -r '.node.nodes[].nodes | .[] | select(.key | endswith("current")) | { dogu: .key | sub("\/dogu\/(?<name>.*)\/current";"\(.name)"), version: .value } | [.dogu, .version] | @tsv' |
    while IFS=$'\t' read -r dogu version; do
      local doguDescriptor logoutUri saType
      doguDescriptor="$(echo "${etcdDoguResponse}" | jq -r ".node.nodes[].nodes | .[] | select(.key == \"/dogu/${dogu}/${version}\") | .value")"
      logoutUri="$(echo "${doguDescriptor}" | jq -r '.Properties.logoutUri')"
      if [[ "${logoutUri}" != "null" ]]; then
        echo "Migrating logout URI for dogu '${dogu}'"
        saType="$(echo "${doguDescriptor}" | jq -r 'try (.ServiceAccounts | map(select(.Type == "cas")) | .[].Params[0] // "cas") catch "cas"')"
        doguctl config "service_accounts/${saType}/${dogu}/logout_uri" "${logoutUri}"
      fi
    done

  echo "Migrating logout URIs... Done!"
}

migrateServiceAccounts() {
  echo "Migrating service accounts..."

  migratePortainerServiceAccount

  if [[ -z "${ECOSYSTEM_MULTINODE+x}" || "${ECOSYSTEM_MULTINODE}" == "false" ]]; then
    migrateServiceAccountsToFolders
    migrateLogoutUri
  fi

  echo "Migrating service accounts... Done!"
}

runPostUpgrade() {
  FROM_VERSION="${1}"
  TO_VERSION="${2}"

  NODE_MASTER_FILE='/etc/ces/node_master'

  echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ..."

  checkSameVersion
  removeDeprecatedKeys
  migrateServiceAccounts

  echo "Set registry flag so startup script can start afterwards..."
  doguctl state "upgrade done"
  doguctl config --rm "local_state"

  echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ... Done!"
}

##### Functions definition done; Executing post-upgrade now

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  runPostUpgrade "$@"
fi