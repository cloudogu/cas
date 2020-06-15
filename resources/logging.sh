#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# logging behaviour can be configured in logging/root with the following options <ERROR,WARN,INFO,DEBUG>
DEFAULT_LOGGING_KEY="logging/root"
LOG_LEVEL_ERROR="ERROR"; LOG_LEVEL_WARN="WARN"; LOG_LEVEL_INFO="INFO"; LOG_LEVEL_DEBUG="DEBUG"

# list of accepted log levels
VALID_LOG_LEVELS=( "${LOG_LEVEL_ERROR}" "${LOG_LEVEL_WARN}" "${LOG_LEVEL_INFO}" "${LOG_LEVEL_DEBUG}" )
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

  logLevelExitCode=0
  logLevel=$(doguctl config "${DEFAULT_LOGGING_KEY}") || logLevelExitCode=$?

  if [[ ${logLevelExitCode} -ne 0 ]]; then
    if [[ "${logLevel}" =~ "100: Key not found" ]]; then
      echo "${SCRIPT_LOG_PREFIX} Did not find root log level. Log level will default to ${DEFAULT_LOG_LEVEL}"
      return
    else
      echo "ERROR: ${SCRIPT_LOG_PREFIX} Error while accessing registry key ${DEFAULT_LOGGING_KEY}. Command returned with ${logLevelExitCode}: ${logLevel}"
      doguctl state "ErrorRootLogLevelKey"
      sleep 3000
      exit 2
    fi
  fi

  # fast return
  if containsValidLogLevel "${logLevel}" ; then
    return
  fi

  # Start weird log lovel handling
  # check empty string because "config --default" accepts a set key with an empty value as a valid value.
  if [[ "${logLevel}" == "" ]]; then
    echo "${SCRIPT_LOG_PREFIX} Found empty root log level. Setting log level default to ${DEFAULT_LOG_LEVEL}"
    # note the quotations to force bash to use it as the first argument.
    resetDoguLogLevel "${logLevel}" "${DEFAULT_LOG_LEVEL}"
    return
  fi

  uppercaseLogLevel=${logLevel^^}
  if [[ "${logLevel}" != "${uppercaseLogLevel}" ]]; then
    echo "${SCRIPT_LOG_PREFIX} Log level contains lowercase characters. Converting '${logLevel}' to '${uppercaseLogLevel}'..."
    if containsValidLogLevel "${uppercaseLogLevel}" ; then
      echo "${SCRIPT_LOG_PREFIX} Log level seems valid..."
      resetDoguLogLevel "${logLevel}" "${uppercaseLogLevel}"
      return
    fi
  fi

  # Things really got weird: Falling back to default
  echo "${SCRIPT_LOG_PREFIX} Found unsupported log level ${logLevel}. These log levels are supported: ${VALID_LOG_LEVELS[*]}"
  resetDoguLogLevel "${logLevel}" "${DEFAULT_LOG_LEVEL}"
  return
}

function containsValidLogLevel() {
  foundLogLevel="${1}"

  for value in "${VALID_LOG_LEVELS[@]}"; do
    if [[ "${value}" == "${foundLogLevel}" ]]; then
      return 0
    fi
  done

  return 1
}

function resetDoguLogLevel() {
  oldLogLevel="${1}"
  targetLogLevel="${2}"
  echo "${SCRIPT_LOG_PREFIX} Resetting dogu log level from '${oldLogLevel}' to '${targetLogLevel}'..."
  doguctl config "${DEFAULT_LOGGING_KEY}" "${targetLogLevel}"
}

echo "Starting log level mapping..."
validateDoguLogLevel
mapDoguLogLevel
echo "Log level mapping ended successfully..."

echo "Rendering logging configuration..."
doguctl template ${CAS_LOGGING_TEMPLATE} ${CAS_LOGGING}
doguctl template ${TOMCAT_LOGGING_TEMPLATE} ${TOMCAT_LOGGING}