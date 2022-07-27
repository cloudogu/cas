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

# Creates the regular expression for the password policy.
# The various requirements for the password policy can be configured in etcd.
function createPasswordPolicyPattern() {
  MUST_CONTAIN_CAPITAL_LETTER=true
  MUST_CONTAIN_LOWER_CASE_LETTER=true
  MUST_CONTAIN_DIGIT=true
  MUST_CONTAIN_SPECIAL_CHARACTER=true

  MIN_LENGTH=14

  echo "Create password policy pattern"
  PASSWORD_POLICY_PATTERN='^'

  if $MUST_CONTAIN_CAPITAL_LETTER
  then
    echo "Password must contain a capital letter"
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"(?=.*[A-Z\u00c4\u00d6\u00dc])"
  fi

  if $MUST_CONTAIN_LOWER_CASE_LETTER
  then
    echo "Password must contain a lower case letter"
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"(?=.*[a-z\u00e4\u00f6\u00fc])"
  fi

  if $MUST_CONTAIN_DIGIT
  then
    echo "Password must contain a digit"
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"(?=.*[0-9])"
  fi

  if $MUST_CONTAIN_SPECIAL_CHARACTER
  then
    echo "Password must contain a special character"
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"(?=.*[^a-zA-Z0-9\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df])"
  fi

  PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}'[\\S]'

  if [ "$MIN_LENGTH" -gt "0" ];
  then
    echo "Password must have a minimum length of ${MIN_LENGTH} characters"
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"{"$MIN_LENGTH",}"
  else
    PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"{1,}"
  fi

  PASSWORD_POLICY_PATTERN=${PASSWORD_POLICY_PATTERN}"$"

  echo "Password Policy is ${PASSWORD_POLICY_PATTERN}"

  export PASSWORD_POLICY_PATTERN
}

# Sets general configuration option for the cas server
function configureCAS() {
  DOMAIN=$(doguctl config --global domain)
  LDAP_TYPE=$(doguctl config ldap/ds_type)
  if [[ "$LDAP_TYPE" == 'external' ]]; then
    echo "ldap type is external"
    LDAP_BASE_DN=$(doguctl config ldap/base_dn)
    LDAP_BIND_DN=$(doguctl config ldap/connection_dn)
  else
    echo "ldap type is embedded"
    LDAP_BASE_DN="ou=People,o=${DOMAIN},dc=cloudogu,dc=com"
    LDAP_BIND_DN=$(doguctl config -e sa-ldap/username)
  fi

  export LDAP_BASE_DN
  export LDAP_BIND_DN

  LDAP_ENCRYPTION=$(doguctl config ldap/encryption) || LDAP_ENCRYPTION="none" # ssl, sslAny, startTLS, startTLSAny or none
  if [[ "$LDAP_ENCRYPTION" == 'startTLS' || "$LDAP_ENCRYPTION" == 'startTLSAny' ]]; then
     LDAP_STARTTLS='true'
     LDAP_PROTOCOL='ldap'
  elif [[ "$LDAP_ENCRYPTION" == 'ssl' || "$LDAP_ENCRYPTION" == 'sslAny' ]]; then
     LDAP_STARTTLS='false'
     LDAP_PROTOCOL='ldaps'
  else # none or ""
     LDAP_STARTTLS='false'
     LDAP_PROTOCOL='ldap'
  fi

  if [[ "$LDAP_ENCRYPTION" == 'startTLSAny' || "$LDAP_ENCRYPTION" == 'sslAny' ]]; then
     LDAP_TRUST_MANAGER='ANY'
  else
     LDAP_TRUST_MANAGER='DEFAULT'
  fi

  export LDAP_STARTTLS
  export LDAP_PROTOCOL
  export LDAP_TRUST_MANAGER

  LDAP_ATTRIBUTE_USERNAME=$(doguctl config ldap/attribute_id)
  LDAP_SEARCH_FILTER="(&$(doguctl config ldap/search_filter)($LDAP_ATTRIBUTE_USERNAME={user}))"
  export LDAP_SEARCH_FILTER

  createPasswordPolicyPattern

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

