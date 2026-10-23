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

@test "encryptPlainTextTOTPKey should re-encrypt a plain text key with its value unchanged" {
  source /workspace/resources/util.sh

  mock_set_output "${doguctl}" "error decrypting registry value" 1
  mock_set_status "${doguctl}" 1 1
  mock_set_output "${doguctl}" "plain-text-key" 2
  mock_set_status "${doguctl}" 0 3

  run encryptPlainTextTOTPKey "totp/signing_key"

  assert_success
  assert_equal "$(mock_get_call_args "${doguctl}" "2")" "config totp/signing_key"
  assert_equal "$(mock_get_call_args "${doguctl}" "3")" "config --encrypted totp/signing_key plain-text-key"
  assert_line "Found plain-text totp/signing_key! Encrypt it..."
}

@test "ensureTOTPKey should generate a key that is missing" {
  source /workspace/resources/util.sh

  mock_set_output "${doguctl}" "no value provided for key" 1
  mock_set_status "${doguctl}" 1 1
  mock_set_status "${doguctl}" 1 2

  run ensureTOTPKey "totp/scratch_codes/encryption_key"

  assert_success
  assert_line "generating totp/scratch_codes/encryption_key"
  # the third call writes the generated key back, encrypted
  assert_equal "$(mock_get_call_num "${doguctl}")" "3"
}

@test "ensureTOTPKey should keep an existing encrypted key" {
  source /workspace/resources/util.sh

  # call 1: migration check, call 2: generation marker -- both report a readable key
  mock_set_status "${doguctl}" 0 1
  mock_set_status "${doguctl}" 0 2

  run ensureTOTPKey "totp/encryption_key"

  assert_success
  assert_equal "$(mock_get_call_num "${doguctl}")" "2"
  # no output at all proves that neither the migration nor the generation branch ran
  assert_output ""
}