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

# create a mapping because apache uses different log levels than log4j eg. ERROR=>SEVERE
function mapDoguLogLevel() {
  echo "${SCRIPT_LOG_PREFIX} Mapping dogu specifig log level"
  currentLogLevel=$(doguctl config --default "${DEFAULT_LOG_LEVEL}" "${DEFAULT_LOGGING_KEY}")
  if [[ "$?" != "0" ]] ; then
    currentLogLevel="not found"
  fi
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

# validate key entries and correct them if needed
function validateDoguLogLevel() {
  echo "${SCRIPT_LOG_PREFIX} Validate root log level"
  logLevel=$(doguctl config --default "${DEFAULT_LOG_LEVEL}" "${DEFAULT_LOGGING_KEY}")
  # "config --default" accepts a set key with an empty value
  if [[ "${logLevel}" == "" ]]; then
    echo "${SCRIPT_LOG_PREFIX} Did not find missing log level."
    resetDoguLogLevel ${logLevel} ${DEFAULT_LOG_LEVEL}
    return
  fi

  uppercaseLogLevel=${logLevel^^}
  if [[ "${logLevel}" != "${uppercaseLogLevel}" ]]; then
    echo "${SCRIPT_LOG_PREFIX} Found lowercase log level. Converting ${logLevel} to ${uppercaseLogLevel}..."
  fi

  # The added spaces in this test avoid partial matches. F. ex., the invalid value "ERR" could falsely match "ERROR"
  if [[ " ${VALID_LOG_LEVELS[@]} " =~ " ${uppercaseLogLevel} " ]]; then
    echo "${SCRIPT_LOG_PREFIX} Using log level ${uppercaseLogLevel}..."
    return
  else
    echo "${SCRIPT_LOG_PREFIX} Found unsupported log level ${uppercaseLogLevel}. These log levels are supported: ${VALID_LOG_LEVELS[@]}"
    resetDoguLogLevel ${uppercaseLogLevel} ${DEFAULT_LOG_LEVEL}
    return
  fi
}

function resetDoguLogLevel() {
  targetLogLevel=${2}
  echo "${SCRIPT_LOG_PREFIX} Resetting dogu log level from ${1} to ${targetLogLevel}..."
  doguctl config "${DEFAULT_LOGGING_KEY}" "${targetLogLevel}"
}

echo "Starting log level mapping..."
validateDoguLogLevel
mapDoguLogLevel
echo "Log level mapping ended successfully..."

echo "Rendering logging configuration..."
doguctl template ${TOMCAT_LOGGING_TEMPLATE} ${TOMCAT_LOGGING}
