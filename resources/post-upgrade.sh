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

  # Parse services that have registered a service-account and their secret hash from ETCD response.
  # Formatted as tab-separated-values these can be iterated over in bash.
  servicesFile="$(mktemp)"
  jq -r ".node // {} | .nodes // [] | .[] | select(.dir | not) | { service: .key | sub(\".*/${saType}/(?<name>[^/]*)$\";\"\(.name)\"), clientSecretHash: .value } | [.service, .clientSecretHash] | @tsv" < "${outFile}" > ${servicesFile}
  # Read all lines from the file into an array
  mapfile -t lines < "${servicesFile}"
  for line in "${lines[@]}"; do
    IFS=$'\t' read -r service clientSecretHash <<< "$line"
    echo "Migrating service account directory for '${service}' with ${clientSecretHash}"
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

  # Parse dogus and their currently installed versions from recursive ETCD response.
  # They are outputted as tab-separated-values, which we iterate over in bash.
  echo "${etcdDoguResponse}" | jq -r '.node // {} | .nodes // [] | .[].nodes // [] | .[] | select(.key | endswith("current")) | { dogu: .key | sub("\/dogu\/(?<name>.*)\/current";"\(.name)"), version: .value } | [.dogu, .version] | @tsv' |
    while IFS=$'\t' read -r dogu version; do
      local doguDescriptor logoutUri saType
      # Read dogu descriptor of the dogu with the specified version from recursive ETCD response.
      doguDescriptor="$(echo "${etcdDoguResponse}" | jq -r ".node // {} | .nodes // [] | .[].nodes // [] | .[] | select(.key == \"/dogu/${dogu}/${version}\") | .value")"
      logoutUri="$(echo "${doguDescriptor}" | jq -r '.Properties.logoutUri')"
      if [[ "${logoutUri}" != "null" ]]; then
        echo "Migrating logout URI for dogu '${dogu}'"
        # Get service accounts of type 'cas' and return the first param if it exists, otherwise return 'cas'.
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

migrateServicesFromETCD() {
  echo "Start to migrate services from etcd to json registry..."

  if [[ $(doguctl config "service_accounts/migrated" -d "false") == "true" ]]; then
    echo "Service accounts have already been migrated to json service registry, skip migration."
    return 0
  fi

  if [[ "$(doguctl multinode)" = "false" ]]; then
    echo "SingleNode environment detected, migrate legacy cas services"
    migrateLegacyServicesFromETCD
  fi

  # Declare associative arrays to hold values for each application
  declare -A types
  declare -A secrets
  declare -A logout_uris

  keys=$(doguctl ls service_accounts || true)

  if [ -z "$keys" ]; then
    echo "did not find any service_accounts, skipping service migration..."
    doguctl config "service_accounts/migrated" "true"
    return 0
  fi

  # Loop through keys representing service values
  for key in $keys; do
      # Check if the key ends with 'secret', 'created' or 'logout_uri
      if [[ $key == *"/secret" || $key == *"/created" || $key == *"/logout_uri" ]]; then
          # Extract the type (two levels above 'secret' or 'created')
          type=$(echo "$key" | awk -F'/' '{print $(NF-2)}')

          # Extract the application name (entry before 'secret', 'created' or 'logout_uri')
          app=$(echo "$key" | awk -F'/' '{print $(NF-1)}')

          # Get the value associated with this key
          value=$(doguctl config "$key")

          echo "Type: $type"
          echo "Application: $app"
          echo "Key: $key"
          echo "Value: $value"
          echo "--------------------"

          # Store values based on the key type
          types["$app"]="$type"
          if [[ $key == *"/secret" ]]; then
              secrets["$app"]="$value"
          elif [[ $key == *"/logout_uri" ]]; then
              logout_uris["$app"]="$value"
          fi
      fi
  done

  # migrate extracted services to json registry
  for app in "${!types[@]}"; do
      account_type="${types[$app]}"
      logout_uri=${logout_uris[$app]:-}    # This will be empty if not set
      secret=${secrets[$app]:-}            # This will be empty if not set

      if [ -n "$logout_uri" ]; then
        # Assuming the random secret is in the generated file, replace it with your secret
        ./create-sa.sh "$account_type" "$logout_uri" "$app"
      else
        ./create-sa.sh "$account_type" "$app"
      fi

      echo "created json service $app from type $account_type"

      # Now replace the random secret in the generated JSON with your extracted secret
      if [ -n "$secret" ]; then
        # Assume the created file name format is <app>-<id>.json
        json_output_file=$(ls -1 "$SERVICE_REGISTRY_PRODUCTION"/${app}-*.json | head -n 1)  # Get the most recently created file

        if [[ -f "$json_output_file" ]]; then
          # Assuming the random secret is in the generated file, replace it with secret from migration
          sed -i "s|\"clientSecret\": \".*\"|\"clientSecret\": \"$secret\"|" "$json_output_file"
        else
          echo "Error: JSON file for $app not found."
        fi
      fi

      echo "Service configuration for $app created."
  done

  doguctl config "service_accounts/migrated" "true"

  echo "Migration completed. Individual files created for each application."
}

migrateLegacyServicesFromETCD() {
  basePath="/tmp/legacyEtcdCasMigration"

  mkdir -p "${basePath}"

  # Get all services listed in etcd
  wget -O "${basePath}/ces.json" "http://$(getEtcdEndpoint):4001/v2/keys/dogu_v2?recursive=true"

  # extract the keys from json etcd response
  jq '.node.nodes' "${basePath}/ces.json" > "${basePath}/nodes.json"
  # filter services that have a dependency on cas
  jq -r '.[] | .nodes[] | select(.value | contains("\"name\":\"cas\"")) | .value' "${basePath}/nodes.json" | jq -s '.' > "${basePath}/filter.json"
  # filter services that don't have defined a service account for cas within dogu.json
  jq '[.[] | select((.ServiceAccounts == null) or (.ServiceAccounts? | map(select(.Type == "cas")) | length) == 0)]' "${basePath}/filter.json" > "${basePath}/exclude.json"
  # extract name from dogu.json
  jq -r '.[] | .Name ' "${basePath}/exclude.json" > "${basePath}/migration.json"
  # delete namespace from name, sort and delete duplicates
  sed 's#.*/##' "${basePath}/migration.json" | sort | uniq > "${basePath}/migrationCandidates.txt"

  candidateFile="${basePath}/migrationCandidates.txt"

  # Check if names file exists
  if [[ ! -f "$candidateFile" ]]; then
      echo "candidate file not found: $candidateFile" >&2
      exit 1
  fi

  # Read each line from the file
  while IFS= read -r name; do
      echo "checking ${name}..."
      #Check whether service is installed and has not migrated already
      status_code=$(wget --spider -S "http://$(getEtcdEndpoint):4001/v2/keys/dogu_v2/${name}/current" 2>&1 | grep "HTTP/" | awk '{print $2}'; exit 0)
      if [[ "$status_code" -eq 200 ]]; then
        # We do not need to consider the LogoutUri as it has already been migrated in a previous step.
        doguctl config "service_accounts/cas/${name}/created" "true"
        echo "set service account entry in etcd for service ${name}"
      else
        echo "dogu ${name} is currently not installed, skip migration for it..."
      fi
  done < "$candidateFile"

  # Delete temporary migration relevant files
  rm -r "${basePath}"

  echo "Legacy service account migration done.\n"
}

runPostUpgrade() {
  FROM_VERSION="${1}"
  TO_VERSION="${2}"

  NODE_MASTER_FILE='/etc/ces/node_master'

  echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ..."

  checkSameVersion
  removeDeprecatedKeys
  migrateServiceAccounts
  migrateServicesFromETCD

  echo "Set registry flag so startup script can start afterwards..."
  doguctl state "upgrade done"
  doguctl config --rm "local_state"

  echo "Executing CAS post-upgrade from ${FROM_VERSION} to ${TO_VERSION} ... Done!"
}

##### Functions definition done; Executing post-upgrade now

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  runPostUpgrade "$@"
fi