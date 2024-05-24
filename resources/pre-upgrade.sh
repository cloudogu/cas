#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

FROM_VERSION=""
TO_VERSION=""

# versionXLessOrEqualThanY returns true if X is less than or equal to Y; otherwise false
function versionXLessOrEqualThanY() {
  local sourceVersion="${1}"
  local targetVersion="${2}"

  if [[ "${sourceVersion}" == "${targetVersion}" ]]; then
    return 0
  fi

  declare -r semVerRegex='([0-9]+)\.([0-9]+)\.([0-9]+)(\.([0-9]+))?-([0-9]+)'

  local sourceMajor=0
  local sourceMinor=0
  local sourceBugfix=0
  local sourceHotfix=0
  local sourceDogu=0
  local targetMajor=0
  local targetMinor=0
  local targetBugfix=0
  local targetHotfix=0
  local targetDogu=0

  if [[ ${sourceVersion} =~ ${semVerRegex} ]]; then
    sourceMajor=${BASH_REMATCH[1]}
    sourceMinor="${BASH_REMATCH[2]}"
    sourceBugfix="${BASH_REMATCH[3]}"
    # BASH_REMATCH[4] includes the . delimiter for technical reasons
    sourceHotfix="${BASH_REMATCH[5]}"
    sourceDogu="${BASH_REMATCH[6]}"
  else
    echo "ERROR: source dogu version ${sourceVersion} does not seem to be a semantic version"
    exit 1
  fi

  if [[ ${targetVersion} =~ ${semVerRegex} ]]; then
    targetMajor=${BASH_REMATCH[1]}
    targetMinor="${BASH_REMATCH[2]}"
    targetBugfix="${BASH_REMATCH[3]}"
    # BASH_REMATCH[4] includes the . delimiter for technical reasons
    targetHotfix="${BASH_REMATCH[5]}"
    targetDogu="${BASH_REMATCH[6]}"
  else
    echo "ERROR: target dogu version ${targetVersion} does not seem to be a semantic version"
    exit 1
  fi

  if [[ $((sourceMajor)) -lt $((targetMajor)) ]]; then
    return 0
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -lt $((targetMinor)) ]]; then
    return 0
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -le $((targetMinor)) && $((sourceBugfix)) -lt $((targetBugfix)) ]]; then
    return 0
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -le $((targetMinor)) && $((sourceBugfix)) -le $((targetBugfix)) && $((sourceHotfix)) -lt $((targetHotfix)) ]]; then
    return 0
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -le $((targetMinor)) && $((sourceBugfix)) -le $((targetBugfix)) && $((sourceHotfix)) -le $((targetHotfix)) && $((sourceDogu)) -lt $((targetDogu)) ]]; then
    return 0
  fi

  return 1
}

runPreUpgrade() {
  FROM_VERSION="${1}"
  TO_VERSION="${2}"

  echo "Executing CAS pre-upgrade from ${FROM_VERSION} to ${TO_VERSION}"

  if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
    echo "FROM and TO versions are the same; Exiting..."
    exit 0
  fi

  FQDN="$(cat /etc/ces/node_master)"
  echo "FQDN: ${FQDN}"

  # Every version above 6.5.3-6 needs a service account that can read and write. To ensure it is not readonly like the
  # versions below we delete the old service account.
  echo "${FROM_VERSION} to ${TO_VERSION}"
  LAST_VERSION_WITH_READONLY_SERVICE_ACCOUNT="6.5.3-6"
  if versionXLessOrEqualThanY "${FROM_VERSION}" "${LAST_VERSION_WITH_READONLY_SERVICE_ACCOUNT}"; then
    echo "FROM version <= 6.5.3-6"
    if ! versionXLessOrEqualThanY "${TO_VERSION}" "${LAST_VERSION_WITH_READONLY_SERVICE_ACCOUNT}"; then
      echo "TO version > 6.5.3-6"
      echo "Upgrade FROM version below v6.5.3-6 TO ${TO_VERSION} -> delete old ldap service account"
      # This is a workaround because `doguctl -rm` cannot delete folders and the cesapp checks if the path
      # `sa-<servicename> is present. Deleting the sa-ldap/username and sa-ldap/password keys does therefore not work.
      curl "http://${FQDN}:4001/v2/keys/config/cas/sa-ldap?recursive=true" -XDELETE
      echo "Service account has been removed successfully."
    fi
  fi

  echo "Set registry flag so startup script waits for post-upgrade to finish..."
  doguctl state "upgrading"

  echo "CAS pre-upgrade done"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  runPreUpgrade "$@"
fi
