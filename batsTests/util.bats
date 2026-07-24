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

@test "validate additional toc entries should ignore empty configuration" {
  source /workspace/resources/util.sh

  mock_set_output "${doguctl}" "EMPTY" 1

  run validateAdditionalTOCEntries

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" "1"
}

@test "validate additional toc entries should allow unique entries" {
  source /workspace/resources/util.sh

  mock_set_output "${doguctl}" "alpha beta gamma" 1

  run validateAdditionalTOCEntries

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" "1"
}

@test "validate additional toc entries should fail on duplicate entries" {
  source /workspace/resources/util.sh

  mock_set_output "${doguctl}" "alpha beta alpha" 1

  run validateAdditionalTOCEntries

  assert_failure
  assert_line "ERROR: The doguctl config key certificate/additional/toc must not contain duplicates. Duplicate entry: alpha"
  assert_equal "$(mock_get_call_num "${doguctl}")" "1"
}
