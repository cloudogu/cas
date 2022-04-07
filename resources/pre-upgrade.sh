#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

FROM_VERSION="${1}"
TO_VERSION="${2}"

# versionXLessOrEqualThanY returns true if X is less than or equal to Y; otherwise false
function versionXLessOrEqualThanY() {
  local sourceVersion="${1}"
  local targetVersion="${2}"

  if [[ "${sourceVersion}" == "${targetVersion}" ]]; then
    return 0
  fi

  declare -r semVerRegex='([0-9]+)\.([0-9]+)\.([0-9]+)-([0-9]+)'

   sourceMajor=0
   sourceMinor=0
   sourceBugfix=0
   sourceDogu=0
   targetMajor=0
   targetMinor=0
   targetBugfix=0
   targetDogu=0

  if [[ ${sourceVersion} =~ ${semVerRegex} ]]; then
    sourceMajor=${BASH_REMATCH[1]}
    sourceMinor="${BASH_REMATCH[2]}"
    sourceBugfix="${BASH_REMATCH[3]}"
    sourceDogu="${BASH_REMATCH[4]}"
  else
    echo "ERROR: source dogu version ${sourceVersion} does not seem to be a semantic version"
    exit 1
  fi

  if [[ ${targetVersion} =~ ${semVerRegex} ]]; then
    targetMajor=${BASH_REMATCH[1]}
    targetMinor="${BASH_REMATCH[2]}"
    targetBugfix="${BASH_REMATCH[3]}"
    targetDogu="${BASH_REMATCH[4]}"
  else
    echo "ERROR: target dogu version ${targetVersion} does not seem to be a semantic version"
    exit 1
  fi

  if [[ $((sourceMajor)) -lt $((targetMajor)) ]] ; then
    return 0;
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -lt $((targetMinor)) ]] ; then
    return 0;
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -le $((targetMinor)) && $((sourceBugfix)) -lt $((targetBugfix)) ]] ; then
    return 0;
  fi
  if [[ $((sourceMajor)) -le $((targetMajor)) && $((sourceMinor)) -le $((targetMinor)) && $((sourceBugfix)) -le $((targetBugfix)) && $((sourceDogu)) -lt $((targetDogu)) ]] ; then
    return 0;
  fi

  return 1
}

echo "Executing CAS pre-upgrade from ${FROM_VERSION} to ${TO_VERSION}"

if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
  echo "FROM and TO versions are the same; Exiting..."
  exit 0
fi


FQDN="$(doguctl config --global fqdn)"
echo "FQDN: ${FQDN}"

# Every version above 6.3.7-4 needs a service account that can read and write. To ensure it is not readonly like the
# versions below we delete the old service account.
echo "${FROM_VERSION} to ${TO_VERSION}"
echo "check if FROM version <= 6.3.7-4"
SERVICE_ACCOUNT_RECREATE_VERSION="6.3.7-4"
if versionXLessOrEqualThanY "${FROM_VERSION}" "${SERVICE_ACCOUNT_RECREATE_VERSION}" ; then
  echo "check if TO version > 6.3.7-4"
        if ! versionXLessOrEqualThanY "${TO_VERSION}" "${SERVICE_ACCOUNT_RECREATE_VERSION}" ; then
                echo "Upgrade FROM version below v6.3.7-4 TO a version above v6.3.7-4 -> delete old service account"
                # This is a workaround because `doguctl -rm` cannot delete folders and the cesapp checks if the path
                # `sa-<servicename> is present. Deleting the sa-ldap/username and sa-ldap/password keys does therefore not work.
                curl "http://${FQDN}:4001/v2/keys/config/cas/sa-ldap?recursive=true" -XDELETE
                echo "remove sa has run"
        fi
fi



echo "Set registry flag so startup script waits for post-upgrade to finish..."
doguctl state "upgrading"

echo "CAS pre-upgrade done"

