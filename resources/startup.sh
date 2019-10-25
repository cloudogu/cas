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

MESSAGES_PROPERTIES_FILE="/opt/apache-tomcat/webapps/cas/WEB-INF/classes/messages.properties"

echo "Getting ldap settings for template..."
LDAP_TYPE=$(doguctl config ldap/ds_type)

FORGOT_PASSWORD_TEXT=$(cfg_or_default 'forgot_password_text' '')

if [[ "$LDAP_TYPE" == 'external' ]]; then
  echo "ldap type is external"
else
  echo "ldap type is embedded"
fi

echo "Render properties template"
doguctl template /opt/apache-tomcat/cas.properties.tpl /opt/apache-tomcat/webapps/cas/WEB-INF/cas.properties


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

echo "Starting cas..."
exec su - cas -c "${CATALINA_SH} run"
