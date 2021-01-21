#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

SERVICE="$1"

if [ X"${SERVICE}" = X"" ]; then
    echo "usage remove-sa.sh servicename"
    exit 1
fi

echo "Removing service_accounts/${SERVICE} key..."
doguctl config --rm "service_accounts/${SERVICE}"