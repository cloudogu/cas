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

# logging behaviour can be configured in logging/root with the following options <ERROR,WARN,INFO,DEBUG>
DEFAULT_LOGGING_KEY="logging/root"
LOG_LEVEL_ERROR="ERROR"; LOG_LEVEL_WARN="WARN"; LOG_LEVEL_INFO="INFO"; LOG_LEVEL_DEBUG="DEBUG"

# list of accepted log levels
DEFAULT_LOG_LEVEL=${LOG_LEVEL_WARN}

# logging configuration used to configure the apache-tomcat logging mechanism
TOMCAT_LOGGING_TEMPLATE="/opt/apache-tomcat/conf/logging.properties.tpl"
TOMCAT_LOGGING="/opt/apache-tomcat/conf/logging.properties"
SCRIPT_LOG_PREFIX="Log level mapping:"

CAS_LOGGING_TEMPLATE="/etc/cas/config/log4j2.xml.tpl"
CAS_LOGGING="/etc/cas/config/log4j2.xml"

# create a mapping because apache uses different log levels than log4j eg. ERROR=>SEVERE
function mapDoguLogLevel() {
  echo "${SCRIPT_LOG_PREFIX} Mapping dogu specific log level"
  currentLogLevel=$(doguctl config --default "${DEFAULT_LOG_LEVEL}" "${DEFAULT_LOGGING_KEY}")

  echo "${SCRIPT_LOG_PREFIX} Mapping ${currentLogLevel} to Catalina"
  case "${currentLogLevel}" in
    "${LOG_LEVEL_ERROR}")
      export CATALINA_LOGLEVEL="SEVERE"
    ;;
    "${LOG_LEVEL_INFO}")
      export CATALINA_LOGLEVEL="INFO"
    ;;
    "${LOG_LEVEL_DEBUG}")
      export CATALINA_LOGLEVEL="FINE"
    ;;
    *)
      echo "${SCRIPT_LOG_PREFIX} Falling back to WARNING"
      export CATALINA_LOGLEVEL="WARNING"
    ;;
  esac
}

function validateDoguLogLevel() {
  echo "${SCRIPT_LOG_PREFIX} Validate root log level"

  validateExitCode=0
  doguctl validate "${DEFAULT_LOGGING_KEY}" || validateExitCode=$?

  if [[ ${validateExitCode} -ne 0 ]]; then
      echo "${SCRIPT_LOG_PREFIX} ERROR: The loglevel configured in ${DEFAULT_LOGGING_KEY} is invalid."
      exit 1
  fi

  return
}

echo "Starting log level mapping..."
validateDoguLogLevel
mapDoguLogLevel
echo "Log level mapping ended successfully..."

echo "Rendering logging configuration..."
doguctl template ${CAS_LOGGING_TEMPLATE} ${CAS_LOGGING}
templatingSuccessful=$?

if [[ "${templatingSuccessful}" != 0 ]];  then
  exitOnErrorWithMessage "invalidConfiguration" "Could not template ${CAS_LOGGING_TEMPLATE} file."
fi

doguctl template ${TOMCAT_LOGGING_TEMPLATE} ${TOMCAT_LOGGING}
templatingSuccessful=$?

if [[ "${templatingSuccessful}" != 0 ]];  then
  exitOnErrorWithMessage "invalidConfiguration" "Could not template ${TOMCAT_LOGGING_TEMPLATE} file."
fi