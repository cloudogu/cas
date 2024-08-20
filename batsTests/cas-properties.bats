#! /bin/bash
# Bind an unbound BATS variables that fail all tests when combined with 'set -o nounset'
export BATS_TEST_START_TIME="0"
export BATSLIB_FILE_PATH_REM=""
export BATSLIB_FILE_PATH_ADD=""

load '/workspace/target/bats_libs/bats-support/load.bash'
load '/workspace/target/bats_libs/bats-assert/load.bash'
load '/workspace/target/bats_libs/bats-mock/load.bash'
load '/workspace/target/bats_libs/bats-file/load.bash'

setup() {
  export STARTUP_DIR=/workspace/resources
  export WORKDIR=/workspace

  export PATH="${PATH}:${BATS_TMPDIR}"
}

teardown() {
  unset doguctl
}

escape(){
  local str="${1}"
  local ESCAPE="${str//\[/\\[}"
  local ESCAPE="${ESCAPE//\]/\\]}"
  echo "${ESCAPE}"
}

# Make sure that all ldaps that are configured in the cas.properties have a pool size of 0.
# This is very important because otherwise the cas can fail to start
@test "all configured ldaps have pool size of zero" {
  PROPFILE="/workspace/resources/etc/cas/config/cas.properties.tpl"

  EXIT_CODE="$(cat "${PROPFILE}" | grep "^ces.ldap-pool-size=0$" > /dev/null; echo $?)"
  ERROR=""
  if [[ "${EXIT_CODE}" != "0" ]]; then
    ERROR="Missing required configuration value 'ces.ldap-pool-size=0'"
  fi
  assert_equal "${ERROR}" ""


  for f in $(cat "${PROPFILE}" |grep "\.ldap\[" | sed 's/\(^[^$]*ldap\[[0-9]*\]\.\).*$/\1/g' |grep -v "#" | sort | uniq); do
    ESCAPED="$(escape "${f}")"
    EXIT_CODE="$(cat "${PROPFILE}" |grep "${ESCAPED}" | grep "=\${ces.ldap-pool-size}" > /dev/null; echo $?)"
    ERROR=""
    if [[ "${EXIT_CODE}" != "0" ]]; then
      ERROR="Configuration '${f}' does not have configured the key 'min-pool-size' with value '\${ces.ldap-pool-size}'"
    fi
    assert_equal "${ERROR}" ""
  done
}
