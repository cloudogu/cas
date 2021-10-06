#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

function checkSameVersion() {
  echo "Checking the CAS versions..."
  if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
    echo "FROM and TO versions are the same; Exiting..."
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

function migrateServiceAccounts() {
  echo "Migrating service service_accounts..."
  VALUE=$(doguctl config service_accounts/portainer --default "")
  echo "Value: ${VALUE}"
  if [[ "${VALUE}" != "" ]]; then
      {
        CLIENT_SECRET="$(doguctl config service_accounts/portainer)"
        doguctl config --remove service_accounts/portainer
        doguctl config service_accounts/oauth/portainer "${CLIENT_SECRET}"
      }
  fi
  echo "Migrating service service_accounts... Done!"
}

FROM_VERSION="${1}"
TO_VERSION="${2}"

echo "Executing CAS pre-upgrade from ${FROM_VERSION} to ${TO_VERSION} ..."

checkSameVersion
removeDeprecatedKeys
migrateServiceAccounts

echo "Executing CAS pre-upgrade from ${FROM_VERSION} to ${TO_VERSION} ... Done!"