#! /bin/bash
# Bind an unbound BATS variables that fail all tests when combined with 'set -o nounset'
export BATS_TEST_START_TIME="0"
export BATSLIB_FILE_PATH_REM=""
export BATSLIB_FILE_PATH_ADD=""

load '/workspace/target/bats_libs/bats-support/load.bash'
load '/workspace/target/bats_libs/bats-assert/load.bash'
load '/workspace/target/bats_libs/bats-mock/load.bash'
load '/workspace/target/bats_libs/bats-file/load.bash'

setup_file() {
  set -o errexit
  set -o nounset
  set -o pipefail

  export STARTUP_DIR=/workspace/resources
  export WORKDIR=/workspace

  export PATH="${BATS_TMPDIR}:${PATH}"
}

setup() {
  source /workspace/resources/create-sa.sh

  doguctl="$(mock_create)"
  export doguctl
  ln -s "${doguctl}" "${BATS_TMPDIR}/doguctl"

  set +o errexit
  set +o nounset
  set +o pipefail
}

teardown() {
  unset doguctl
  rm "${BATS_TMPDIR}/doguctl"
}

@test 'deleteOldServiceAccounts() should try to remove all but cas' {
  mock_set_output "${doguctl}" "DEFAULT" '1'
  mock_set_output "${doguctl}" "my-secret" '2'

  run deleteOldServiceAccounts "my_dogu" "cas"

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" '3'

  assert_equal "$(mock_get_call_args "${doguctl}" '1')" 'config --default DEFAULT service_accounts/oidc/my_dogu/secret'
  assert_equal "$(mock_get_call_args "${doguctl}" '2')" 'config --default DEFAULT service_accounts/oauth/my_dogu/secret'
  assert_equal "$(mock_get_call_args "${doguctl}" '3')" 'config --rm service_accounts/oauth/my_dogu/secret'
}

@test 'deleteOldServiceAccounts() should try to remove all but oidc' {
  mock_set_output "${doguctl}" "DEFAULT" '1'
  mock_set_output "${doguctl}" "my-secret" '2'

  run deleteOldServiceAccounts "my_dogu" "oidc"

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" '3'

  assert_equal "$(mock_get_call_args "${doguctl}" '1')" 'config --default DEFAULT service_accounts/cas/my_dogu/created'
  assert_equal "$(mock_get_call_args "${doguctl}" '2')" 'config --default DEFAULT service_accounts/oauth/my_dogu/secret'
  assert_equal "$(mock_get_call_args "${doguctl}" '3')" 'config --rm service_accounts/oauth/my_dogu/secret'
}

@test 'deleteOldServiceAccounts() should try to remove all but oauth' {
  mock_set_output "${doguctl}" "my-secret" '1'
  mock_set_output "${doguctl}" "DEFAULT" '2'

  run deleteOldServiceAccounts "my_dogu" "oauth"

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" '3'

  assert_equal "$(mock_get_call_args "${doguctl}" '1')" 'config --default DEFAULT service_accounts/cas/my_dogu/created'
  assert_equal "$(mock_get_call_args "${doguctl}" '2')" 'config --rm service_accounts/cas/my_dogu/created'
  assert_equal "$(mock_get_call_args "${doguctl}" '3')" 'config --default DEFAULT service_accounts/oidc/my_dogu/secret'
}