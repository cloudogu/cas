#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# return config value or default value
# param1 config key
# param2 default value
function cfg_or_default {
  if ! VALUE=$(doguctl config "${1}"); then
    VALUE="${2}"
  fi
  echo "${VALUE}"
}

# same as above, but for global config
function global_cfg_or_default {
  if ! VALUE=$(doguctl config --global "${1}"); then
    VALUE="${2}"
  fi
  echo "${VALUE}"
}

# configure logging behaviour using the etcd property logging/root <ERROR,WARN,INFO,DEBUG>

# If an error occurs in logging.sh the whole scripting quits because of -o errexit. Catching the sourced exit code
# leads to an zero exit code which enables further error handling.
loggingExitCode=0
# shellcheck disable=SC1091
source ./logging.sh || loggingExitCode=$?
if [[ ${loggingExitCode} -ne 0 ]]; then
  echo "ERROR: An error occurred during the root log level evaluation.";
  doguctl state "ErrorRootLogLevelMapping"
  sleep 300
  exit 2
fi

echo "Continuing start up..."
MESSAGES_PROPERTIES_FILE="/opt/apache-tomcat/webapps/cas/WEB-INF/classes/messages.properties"
CAS_PROPERTIES_TEMPLATE="/opt/apache-tomcat/cas.properties.conf.tpl"
CAS_PROPERTIES="/opt/apache-tomcat/webapps/cas/WEB-INF/cas.properties"

# use the doguctl templating mechanism for new configuration entries (sed is deprecated and should be migrated)
doguctl template ${CAS_PROPERTIES_TEMPLATE} ${CAS_PROPERTIES}

echo "Getting general variables for templates..."
DOMAIN=$(doguctl config --global domain)
FQDN=$(doguctl config --global fqdn)

echo "Getting ldap settings for template..."
LDAP_TYPE=$(doguctl config ldap/ds_type)
LDAP_HOST=$(doguctl config ldap/host)
LDAP_PORT=$(doguctl config ldap/port)
LDAP_ATTRIBUTE_USERNAME=$(doguctl config ldap/attribute_id)
LDAP_ATTRIBUTE_MAIL=$(doguctl config ldap/attribute_mail)
LDAP_ATTRIBUTE_GROUP=$(doguctl config ldap/attribute_group)
LDAP_ENCRYPTION=$(doguctl config ldap/encryption) || LDAP_ENCRYPTION="none" # ssl, sslAny, startTLS, startTLSAny or none

LDAP_GROUP_BASE_DN=$(cfg_or_default 'ldap/group_base_dn' '')
LDAP_GROUP_SEARCH_FILTER=$(cfg_or_default 'ldap/group_search_filter' '' | sed 's@&@\\\&@g')
LDAP_GROUP_ATTRIBUTE_NAME=$(cfg_or_default 'ldap/group_attribute_name' '')

LDAP_USE_USER_CONNECTION=$(cfg_or_default 'ldap/use_user_connection_to_fetch_attributes' 'true')

# replace & with /& because of sed
LDAP_SEARCH_FILTER=$(echo "(&$(doguctl config ldap/search_filter)($LDAP_ATTRIBUTE_USERNAME={user}))" | sed 's@&@\\\&@g')
FORGOT_PASSWORD_TEXT=$(cfg_or_default 'forgot_password_text' '')

if [[ "$LDAP_TYPE" == 'external' ]]; then
  echo "ldap type is external"
  LDAP_BASE_DN=$(doguctl config ldap/base_dn)
  LDAP_BIND_DN=$(doguctl config ldap/connection_dn)
  LDAP_BIND_PASSWORD=$(doguctl config -e ldap/password | sed 's@/@\\\\/@g')
else
  echo "ldap type is embedded"
  LDAP_BASE_DN="ou=People,o=${DOMAIN},dc=cloudogu,dc=com"
  LDAP_BIND_DN=$(doguctl config -e sa-ldap/username)
  LDAP_BIND_PASSWORD=$(doguctl config -e sa-ldap/password | sed 's@/@\\\\/@g')
fi

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

if [[ "$LDAP_ENCRYPTION" == 'startTLSAny' || "$LDAP_ENCRYPTION" == 'sslAny' ]]; then
  LDAP_TRUST_MANAGER='org.ldaptive.ssl.AllowAnyTrustManager'
else
  LDAP_TRUST_MANAGER='org.ldaptive.ssl.DefaultTrustManager'
fi

echo "Getting stage..."
STAGE=$(global_cfg_or_default 'stage' '')
if [[ "$STAGE" != 'development' ]]; then
  STAGE='production'
fi
REQUIRE_SECURE='true'
if [[ "$STAGE" == 'development' ]]; then
  REQUIRE_SECURE='false'
fi

LOGIN_LIMIT_MAX_NUMBER=$(cfg_or_default 'limit/max_number' '0')
LOGIN_LIMIT_FAILURE_STORE_TIME=$(cfg_or_default 'limit/failure_store_time' '0')
LOGIN_LIMIT_LOCK_TIME=$(cfg_or_default 'limit/lock_time' '0')

echo "Rendering templates..."
sed "s@%DOMAIN%@$DOMAIN@g;\
s@%LDAP_STARTTLS%@$LDAP_STARTTLS@g;\
s@%FQDN%@$FQDN@g;\
s@%STAGE%@$STAGE@g;\
s@%REQUIRE_SECURE%@$REQUIRE_SECURE@g;\
s@%LDAP_PROTOCOL%@$LDAP_PROTOCOL@g;\
s@%LDAP_HOST%@$LDAP_HOST@g;\
s@%LDAP_PORT%@$LDAP_PORT@g;\
s@%LDAP_TRUST_MANAGER%@$LDAP_TRUST_MANAGER@g;\
s@%LDAP_SEARCH_FILTER%@$LDAP_SEARCH_FILTER@g;\
s@%LDAP_BASE_DN%@$LDAP_BASE_DN@g;\
s?%LDAP_BIND_DN%?$LDAP_BIND_DN?g;\
s@%LDAP_BIND_PASSWORD%@$LDAP_BIND_PASSWORD@g;\
s@%LDAP_ATTRIBUTE_USERNAME%@$LDAP_ATTRIBUTE_USERNAME@g;\
s?%LDAP_ATTRIBUTE_MAIL%?$LDAP_ATTRIBUTE_MAIL?g;\
s@%LDAP_ATTRIBUTE_GROUP%@$LDAP_ATTRIBUTE_GROUP@g;\
s@%LDAP_GROUP_BASE_DN%@$LDAP_GROUP_BASE_DN@g;\
s@%LDAP_GROUP_SEARCH_FILTER%@$LDAP_GROUP_SEARCH_FILTER@g;\
s@%LDAP_GROUP_ATTRIBUTE_NAME%@$LDAP_GROUP_ATTRIBUTE_NAME@g;\
s@%LDAP_USE_USER_CONNECTION%@$LDAP_USE_USER_CONNECTION@g;\
s@%LOGIN_LIMIT_MAX_NUMBER%@$LOGIN_LIMIT_MAX_NUMBER@g;\
s@%LOGIN_LIMIT_FAILURE_STORE_TIME%@$LOGIN_LIMIT_FAILURE_STORE_TIME@g;\
s@%LOGIN_LIMIT_LOCK_TIME%@$LOGIN_LIMIT_LOCK_TIME@g"\
 /opt/apache-tomcat/cas.properties.tpl >> /opt/apache-tomcat/webapps/cas/WEB-INF/cas.properties


sed -i '/screen.password.forgotText=/d' ${MESSAGES_PROPERTIES_FILE}
if [[ "$FORGOT_PASSWORD_TEXT" ]]; then
    echo "configure forgot password text"
    echo screen.password.forgotText="$FORGOT_PASSWORD_TEXT" >> ${MESSAGES_PROPERTIES_FILE}
fi

echo "Creating truststore, which is used in the setenv.sh..."
create_truststore.sh > /dev/null

if [[ "$LDAP_TYPE" == 'embedded' ]]; then
  echo "Waiting until ldap passed all health checks..."
  echo "wait until ldap passes all health checks"
  if ! doguctl healthy --wait --timeout 120 ldap; then
    echo "timeout reached by waiting of ldap to get healthy"
    exit 1
  fi
fi

doguctl state ready

echo "Starting cas..."
exec su - cas -c "${CATALINA_SH} run"
