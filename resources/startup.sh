#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# shellcheck disable=SC1091
source util.sh

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

# If an error occurs in logging.sh the whole scripting quits because of -o errexit. Catching the sourced exit code
# leads to an zero exit code which enables further error handling.
loggingExitCode=0
# shellcheck disable=SC1091
source ./logging.sh || loggingExitCode=$?
if [[ ${loggingExitCode} -ne 0 ]]; then
  exitOnErrorWithMessage "ErrorRootLogLevelMapping" "ERROR: An error occurred during the root log level evaluation."
fi

echo "Waiting until ldap-mapper passed all health checks..."
if ! doguctl healthy --wait --timeout 120 ldap-mapper; then
  echo "timeout reached by waiting of ldap-mapper to get healthy"
  exit 1
fi

# from utils.sh - configures the cas server
echo "Create general configuration..."
configureCAS
configureLegalURLs

echo "Creating truststore, which is used in the setenv.sh..."
create_truststore.sh > /dev/null

doguctl state ready

echo "Starting cas..."
exec su - cas -c "${CATALINA_SH} run"