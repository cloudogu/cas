#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

echo "                                     ./////,                    "
echo "                                 ./////==//////*                "
echo "                                ////.  ___   ////.              "
echo "                         ,**,. ////  ,////A,  */// ,**,.        "
echo "                    ,/////////////*  */////*  *////////////A    "
echo "                   ////'        \VA.   '|'   .///'       '///*  "
echo "                  *///  .*///*,         |         .*//*,   ///* "
echo "                  (///  (//////)**--_./////_----*//////)   ///) "
echo "                   V///   '°°°°      (/////)      °°°°'   ////  "
echo "                    V/////(////////\. '°°°' ./////////(///(/'   "
echo "                       'V/(/////////////////////////////V'      "

# shellcheck disable=SC1091
source util.sh

# check whether post-upgrade script is still running
while [[ "$(doguctl state)" == "upgrading" ]]; do
  echo "Upgrade script is running. Waiting..."
  sleep 3
done

# If an error occurs in logging.sh the whole scripting quits because of -o errexit. Catching the sourced exit code
# leads to an zero exit code which enables further error handling.
loggingExitCode=0
# shellcheck disable=SC1091
source ./logging.sh || loggingExitCode=$?
if [[ ${loggingExitCode} -ne 0 ]]; then
  exitOnErrorWithMessage "ErrorRootLogLevelMapping" "ERROR: An error occurred during the root log level evaluation."
fi

LDAP_TYPE=$(doguctl config ldap/ds_type)
if [[ "$LDAP_TYPE" == 'embedded' ]]; then
  echo "Waiting until ldap passed all health checks..."
  if ! doguctl healthy --wait --timeout 120 ldap; then
    echo "timeout reached by waiting of ldap to get healthy"
    exit 1
  fi
fi

# from utils.sh - configures the CAS server
echo "Create CAS configuration..."
configureCAS

echo "Creating truststore, which is used in the setenv.sh..."
create_truststore.sh > /dev/null

doguctl state ready

echo "Starting CAS..."
${CATALINA_SH} run