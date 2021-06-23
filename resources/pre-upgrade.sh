#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

FROM_VERSION="${1}"
TO_VERSION="${2}"

echo "Executing CAS pre-upgrade from ${FROM_VERSION} to ${TO_VERSION}"

if [ "${FROM_VERSION}" = "${TO_VERSION}" ]; then
  echo "FROM and TO versions are the same; Exiting..."
  exit 0
fi

echo "Remove deprecated etcd Keys..."
VALUE=$(doguctl config "logging/translation_messages" --default "default")
if [[ "${VALUE}" != "default" ]]; then
  doguctl config --remove "logging/translation_messages"
fi

echo "CAS pre-upgrade done"