#!/bin/bash

set -o errexit
set -o nounset
set -o pipefail

FROM_VERSION="${1}"
TO_VERSION="${2}"

if [[ "${FROM_VERSION}" == 4* ]] && [[ "${TO_VERSION}" == 6* ]]; then
    echo "############################## MAJOR UPGRADE ##############################"
    echo "The upgrade from CAS 4 to CAS 6 is a major upgrade. We tried to prevent complications as far as possible, but can not fully ensure everything will work as it has before."
    echo "Therefore we suggest to backup the EcoSystem before upgrading CAS. We also suggest to upgrade the other dogus to their latest version since these are more tested."
    echo "For additional support or questions, feel free to contact hello@cloudogu.com."
fi