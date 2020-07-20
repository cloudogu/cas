#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# logging behaviour can be configured in logging/root with the following options <ERROR,WARN,INFO,DEBUG>
DEFAULT_LOGGING_KEY="logging/root"
LOG_LEVEL_ERROR="ERROR"; LOG_LEVEL_WARN="WARN"; LOG_LEVEL_INFO="INFO"; LOG_LEVEL_DEBUG="DEBUG"

# list of accepted log levels
DEFAULT_LOG_LEVEL=${LOG_LEVEL_WARN}

# logging configuration used to configure the apache-tomcat logging mechanism
TOMCAT_LOGGING_TEMPLATE="/opt/apache-tomcat/conf/logging.properties.conf.tpl"
TOMCAT_LOGGING="/opt/apache-tomcat/conf/logging.properties"
SCRIPT_LOG_PREFIX="Log level mapping:"

CAS_LOGGING_TEMPLATE="/etc/cas/conf/log4j.xml.tpl"
CAS_LOGGING="/opt/apache-tomcat/webapps/cas/WEB-INF/classes/log4j.xml"

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
      echo "${SCRIPT_LOG_PREFIX} WARNING: The loglevel configured in ${DEFAULT_LOGGING_KEY} is invalid."
      echo "${SCRIPT_LOG_PREFIX} WARNING: Removing misconfigured value."
      doguctl config --rm "${DEFAULT_LOGGING_KEY}"
  fi

  return
}

echo "Starting log level mapping..."
validateDoguLogLevel
mapDoguLogLevel
echo "Log level mapping ended successfully..."

echo "Rendering logging configuration..."
doguctl template ${CAS_LOGGING_TEMPLATE} ${CAS_LOGGING}
doguctl template ${TOMCAT_LOGGING_TEMPLATE} ${TOMCAT_LOGGING}
