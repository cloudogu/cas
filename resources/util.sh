#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# This function prints an error to the console and waits 5 minutes before exiting the process.
# Requires two arguments:
# 1 - Error state
# 2 - Error message
function exitOnErrorWithMessage() {
  errorState=${1}
  errorMessage=${2}
  waitBeforeRestart=300
  echo -e "ERROR: ${errorMessage}. Exiting in ${waitBeforeRestart} seconds..."
  doguctl state "${errorState}"
  sleep "${waitBeforeRestart}"
  exit 2
}

# Sets general configuration option for the cas server
function configureCAS() {
  DOMAIN=$(doguctl config --global domain)
  LDAP_TYPE=$(doguctl config ldap/ds_type)
  if [[ "$LDAP_TYPE" == 'external' ]]; then
    echo "ldap type is external"
    LDAP_BASE_DN=$(doguctl config ldap/base_dn)
  else
    echo "ldap type is embedded"
    LDAP_BASE_DN="ou=People,o=${DOMAIN},dc=cloudogu,dc=com"
  fi

  export LDAP_BASE_DN

  LDAP_ENCRYPTION=$(doguctl config ldap/encryption) || LDAP_ENCRYPTION="none" # ssl, sslAny, startTLS, startTLSAny or none
  if [[ "$LDAP_ENCRYPTION" == 'startTLS' || "$LDAP_ENCRYPTION" == 'startTLSAny' ]]; then
     LDAP_STARTTLS='true'
     LDAP_PROTOCOL='ldap'
  elif [[ "$LDAP_ENCRYPTION" == 'ssl' || "$LDAP_ENCRYPTION" == 'sslAny' ]]; then
     LDAP_STARTTLS='false'
     LDAP_PROTOCOL='ldaps'
  else # none
     LDAP_STARTTLS='false'
     LDAP_PROTOCOL='ldap'
  fi

  export LDAP_STARTTLS
  export LDAP_PROTOCOL

  CAS_PROPERTIES_TEMPLATE="/etc/cas/config/cas.properties.tpl"
  CAS_PROPERTIES="/etc/cas/config/cas.properties"

  doguctl template "${CAS_PROPERTIES_TEMPLATE}" "${CAS_PROPERTIES}"
  templatingSuccessful=$?

  if [[ "${templatingSuccessful}" != 0 ]];  then
    exitOnErrorWithMessage "invalidConfiguration" "Could not template cas.properties.tpl file."
  fi
}

# Sets configured legal URLs for the cas UI
function configureLegalURLs() {
  CUSTOM_MESSAGES_TEMPLATE="/etc/cas/config/custom_messages.properties.tpl"
  CUSTOM_MESSAGES="/etc/cas/config/custom_messages.properties"

  doguctl template "${CUSTOM_MESSAGES_TEMPLATE}" "${CUSTOM_MESSAGES}"
  templatingSuccessful=$?

  if [[ "${templatingSuccessful}" != 0 ]];  then
    exitOnErrorWithMessage "invalidConfiguration" "Could not template custom_messages.properties.tpl file."
  fi
}

