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

  echo "Removing previously created service ${SERVICE} from JSON registry ${SERVICE_REGISTRY}"
  FILES=$(ls "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-*.json 2>/dev/null || echo "")
  # Check if FILES is empty before counting
  if [ -z "$FILES" ]; then
    echo "No files found matching the service ${SERVICE}."
  else
    # Count the number of matching files
    FILE_COUNT=$(echo "$FILES" | wc -l)
    echo "Found $FILE_COUNT file(s) matching service ${SERVICE}."
    rm "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-*.json
    echo "Successfully deleted service ${SERVICE}."
  fi

  echo "Create new sa for ${SERVICE} with account type: ${TYPE}..."

  FQDN=$(doguctl config -g fqdn)
  # escape fqdn to use it within regex
  EFQDN=$(escapeDots "$FQDN")
  SERVICE_ID=$(findNextServiceID "$SERVICE_REGISTRY_PRODUCTION")
  # build logout url
  LOGOUT_URL="https://${FQDN}/${SERVICE}${LOGOUT_URI:-}"

  # Initialize TEMPLATES with base templates
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
    CLIENT_SECRET_HASH=$(echo -n "${CLIENT_SECRET}" | sha256sum | awk '{print $1}')

    # Using `sed` to replace placeholders
    sed -e "s|{{SERVICE}}|$SERVICE|g" \
        -e "s|{{SERVICE_ID}}|$SERVICE_ID|g" \
        -e "s|{{FQDN}}|$EFQDN|g" \
        -e "s|{{TEMPLATES}}|$(IFS=, ; echo "${TEMPLATES[*]}")|g" \
        -e "s|{{CLIENT_SECRET_HASH}}|$CLIENT_SECRET_HASH|g" \
        -e "s|{{SERVICE_CLASS}}|$SERVICE_CLASS|g" \
        -e "s|{{LOGOUT_URL}}|$LOGOUT_URL|g" etc/cas/config/services/oauth-service-template.json.tpl > "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-"${SERVICE_ID}".json

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
        -e "s|{{LOGOUT_URL}}|$LOGOUT_URL|g" etc/cas/config/services/cas-service-template.json.tpl > "$SERVICE_REGISTRY_PRODUCTION"/"${SERVICE}"-"${SERVICE_ID}".json

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
