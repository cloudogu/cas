#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

source util.sh

{
  if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
    echo "usage create-sa.sh account_type [logout_uri] servicename"
    exit 1
  fi

  TYPE="${1}"
  SERVICE="${*: -1}"
  if [ "$#" -eq 3 ]; then
    LOGOUT_URI="${2}"
  fi

  echo "Create sa for ${SERVICE} with account type: ${TYPE}..."

  #FQDN="192.168.56.2"
  FQDN=$(doguctl config -g fqdn)
  # escape fqdn to use it within regex
  EFQDN=$(escape_dots "$FQDN")
  SERVICE_ID=$(find_next_serviceID "$SERVICE_REGISTRY")
  # build logout url
  LOGOUT_URL="https://${FQDN}/${SERVICE}${LOGOUT_URI:-}"

  # Initialize TEMPLATES as an empty array
  TEMPLATES=("BaseService,DefaultAttributeReleasePolicy")

  if [ -n "${LOGOUT_URI+x}" ]; then
    TEMPLATES+=("WithLogoutURI")
    doguctl config "service_accounts/${TYPE}/${SERVICE}/logout_uri" "${LOGOUT_URI}"
  fi

  if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
    TEMPLATES+=("DefaultOAuthService")

    SERVICE_CLASS="org.apereo.cas.support.oauth.services.OAuthRegisteredService"
    if [ "${TYPE}" == "oidc" ] ; then
      SERVICE_CLASS="org.apereo.cas.services.OidcRegisteredService"
    fi

    CLIENT_SECRET=$(doguctl random -l 16)
    #CLIENT_SECRET="secret"
    CLIENT_SECRET_HASH=$(echo -n "${CLIENT_SECRET}" | sha256sum | awk '{print $1}')
    #CLIENT_SECRET_HASH="HASH"

    # Using `sed` to replace placeholders
    sed -e "s|{{SERVICE}}|$SERVICE|g" \
        -e "s|{{SERVICE_ID}}|$SERVICE_ID|g" \
        -e "s|{{FQDN}}|$EFQDN|g" \
        -e "s|{{TEMPLATES}}|$(IFS=, ; echo "${TEMPLATES[*]}")|g" \
        -e "s|{{CLIENT_SECRET_HASH}}|$CLIENT_SECRET_HASH|g" \
        -e "s|{{SERVICE_CLASS}}|$SERVICE_CLASS|g" \
        -e "s|{{LOGOUT_URL}}|$LOGOUT_URL|g" etc/cas/config/services/oauth-service-template.json.tpl > $SERVICE_REGISTRY/${SERVICE}-${SERVICE_ID}.json

    doguctl config "service_accounts/${TYPE}/${SERVICE}/secret" "${CLIENT_SECRET_HASH}"
  elif [ "${TYPE}" == "cas" ]; then
    # Set value `created` because doguctl requires a value to be set
    doguctl config "service_accounts/${TYPE}/${SERVICE}/created" "true"

    # Allow Service to use PGTs as default behavior - could be changed in the future as it is not recommended
    TEMPLATES+=("AllowProxyPolicy")

    # Using `sed` to replace placeholders
    sed -e "s|{{SERVICE}}|$SERVICE|g" \
        -e "s|{{SERVICE_ID}}|$SERVICE_ID|g" \
        -e "s|{{FQDN}}|$EFQDN|g" \
        -e "s|{{TEMPLATES}}|$(IFS=, ; echo "${TEMPLATES[*]}")|g" \
        -e "s|{{LOGOUT_URL}}|$LOGOUT_URL|g" etc/cas/config/services/cas-service-template.json.tpl > $SERVICE_REGISTRY/${SERVICE}-${SERVICE_ID}.json

  else
    echo "only the account_types: oidc, oauth, cas are allowed"
    exit 1
  fi
} >/dev/null 2>&1

# print client-id so that the service-account can be removed again
echo "${TYPE}_client_id: ${SERVICE}"

if [ "${TYPE}" == "oidc" ] || [ "${TYPE}" == "oauth" ]; then
  # print OAuth credentials for the service
  echo "${TYPE}_client_secret: ${CLIENT_SECRET}"
fi
