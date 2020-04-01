#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

# logging behaviour can be configured in /cas/logging/root with the following options <ERROR,WARN,INFO,DEBUG>
DEFAULT_LOGGING_KEY="logging/root"
LOG_LEVEL_ERROR="ERROR"; LOG_LEVEL_WARN="WARN"; LOG_LEVEL_INFO="INFO"; LOG_LEVEL_DEBUG="DEBUG"

# list of accepted log levels
VALID_LOG_LEVELS=( "${LOG_LEVEL_ERROR}" "${LOG_LEVEL_WARN}" "${LOG_LEVEL_INFO}" "${LOG_LEVEL_DEBUG}" )
DEFAULT_LOG_LEVEL=${LOG_LEVEL_WARN}

# logging configuration used to configure the apache-tomcat logging mechanism
TOMCAT_LOGGING_TEMPLATE="/opt/apache-tomcat/conf/logging.properties.conf.tpl"
TOMCAT_LOGGING="/opt/apache-tomcat/conf/logging.properties"

# create a mapping because apache uses different log levels than log4j eg. ERROR=>SEVERE
function mapDoguLogLevel() {
  currentLogLevel=$(doguctl config "${DEFAULT_LOGGING_KEY}")
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
      export CATALINA_LOGLEVEL="WARNING"
    ;;
  esac
}

# validate key entries and correct them if needed
function validateDoguLogLevel() {
  logLevel=$(doguctl config --default "${DEFAULT_LOG_LEVEL}" "${DEFAULT_LOGGING_KEY}")
  # "config --default" accepts a set key with an empty value
  if [[ "${logLevel}" == "" ]]; then
    echo "Did not find missing log level."
    resetDoguLogLevel ${logLevel} ${DEFAULT_LOG_LEVEL}
    return
  fi

  uppercaseLogLevel=${logLevel^^}
  if [[ "${logLevel}" != "${uppercaseLogLevel}" ]]; then
    echo "Found lowercase log level. Converting ${logLevel} to ${uppercaseLogLevel}..."
  fi

  # The added spaces in this test avoid partial matches. F. ex., the invalid value "ERR" could falsely match "ERROR"
  if [[ " ${VALID_LOG_LEVELS[@]} " =~ " ${uppercaseLogLevel} " ]]; then
    echo "Using log level ${uppercaseLogLevel}..."
    return
  else
    echo "Found unsupported log level ${uppercaseLogLevel}. These log levels are supported: ${VALID_LOG_LEVELS[@]}"
    resetDoguLogLevel ${uppercaseLogLevel} ${defaultLogLevel}
    return
  fi
}

function resetDoguLogLevel() {
  targetLogLevel=${2}
  echo "Resetting dogu log level from ${1} to ${targetLogLevel}..."
  doguctl config "${DEFAULT_LOGGING_KEY}" "${targetLogLevel}"
}

echo "Rendering logging configuration..."
validateDoguLogLevel
mapDoguLogLevel

doguctl template ${TOMCAT_LOGGING_TEMPLATE} ${TOMCAT_LOGGING}
