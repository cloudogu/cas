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

  doguctl="$(mock_create)"

  export PATH="${PATH}:${BATS_TMPDIR}"
  ln -s "${doguctl}" "${BATS_TMPDIR}/doguctl"
}

teardown() {
  unset doguctl
  rm "${BATS_TMPDIR}/doguctl"
}

@test "create password policy pattern should create a correct regex when every rule is activated" {
   source /workspace/resources/util.sh

   mock_set_output "${doguctl}" "true" 1
   mock_set_output "${doguctl}" "true" 2
   mock_set_output "${doguctl}" "true" 3
   mock_set_output "${doguctl}" "true" 4
   mock_set_output "${doguctl}" "14" 5

   run createPasswordPolicyPattern

   assert_equal "$(mock_get_call_num "${doguctl}")" "5"

   assert_line "Password must contain a capital letter"
   assert_line "Password must contain a lower case letter"
   assert_line "Password must contain a digit"
   assert_line "Password must contain a special character"
   assert_line "Password must have a minimum length of 14 characters"
   assert_line 'Password Policy is ^(?=.*[A-Z\u00c4\u00d6\u00dc])(?=.*[a-z\u00e4\u00f6\u00fc])(?=.*[0-9])(?=.*[^a-zA-Z0-9\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df])[\\S]{14,}$'
}

@test "create password policy pattern should create a correct regex when no rule is activated" {
   source /workspace/resources/util.sh

   mock_set_output "${doguctl}" "false" 1
   mock_set_output "${doguctl}" "false" 2
   mock_set_output "${doguctl}" "false" 3
   mock_set_output "${doguctl}" "false" 4
   mock_set_output "${doguctl}" "0" 5

   run createPasswordPolicyPattern

   assert_equal "$(mock_get_call_num "${doguctl}")" "5"
   assert_line 'Password Policy is ^[\\S]{1,}$'
}

@test "create password policy pattern should create a correct regex when incorrect values are stored in the etcd" {
   source /workspace/resources/util.sh

   mock_set_output "${doguctl}" "Yes" 1
   mock_set_output "${doguctl}" "Yes" 2
   mock_set_output "${doguctl}" "Yes" 3
   mock_set_output "${doguctl}" "Yes" 4
   mock_set_output "${doguctl}" "Hundert" 5

   run createPasswordPolicyPattern

   assert_equal "$(mock_get_call_num "${doguctl}")" "5"
   assert_line 'Warning: Specified minimum length is not an integer; password minimum length is set to 1'
   assert_line 'Password Policy is ^[\\S]{1,}$'
}