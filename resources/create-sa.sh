#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

{
  SERVICE="$1"

  if [ X"${SERVICE}" = X"" ]; then
      echo "usage create-sa.sh servicename"
      exit 1
  fi

  CLIENT_SECRET=$(doguctl random -l 16)

  CLIENT_SECRET_HASH=$(echo -n "${CLIENT_SECRET}" | sha256sum | awk '{print $1}')

  doguctl config "service_accounts/${SERVICE}" "${CLIENT_SECRET_HASH}"

} >/dev/null 2>&1

# print OAuth credentials for the service
echo "oauth_client_id: ${SERVICE}"
echo "oauth_client_secret: ${CLIENT_SECRET}"