#! /bin/bash
# Bind an unbound BATS variables that fail all tests when combined with 'set -o nounset'
export BATS_TEST_START_TIME="0"
export BATSLIB_FILE_PATH_REM=""
export BATSLIB_FILE_PATH_ADD=""

load '/workspace/target/bats_libs/bats-support/load.bash'
load '/workspace/target/bats_libs/bats-assert/load.bash'
load '/workspace/target/bats_libs/bats-mock/load.bash'
load '/workspace/target/bats_libs/bats-file/load.bash'

JQ_VERSION="1.7.1"

setup_file() {
  set -o errexit
  set -o nounset
  set -o pipefail

  export STARTUP_DIR=/workspace/resources
  export WORKDIR=/workspace

  export NODE_MASTER_FILE="${BATS_TMPDIR}/node_master"
  printf "\t192.168.56.2 \n" > "${NODE_MASTER_FILE}"

  wget -O "${BATS_TMPDIR}/jq" "https://github.com/jqlang/jq/releases/download/jq-${JQ_VERSION}/jq-linux-amd64"
  chmod +x "${BATS_TMPDIR}/jq"

  export PATH="${BATS_TMPDIR}:${PATH}"
}

setup() {
  source /workspace/resources/post-upgrade.sh

  wget="$(mock_create)"
  export wget
  ln -s "${wget}" "${BATS_TMPDIR}/wget"

  doguctl="$(mock_create)"
  export doguctl
  ln -s "${doguctl}" "${BATS_TMPDIR}/doguctl"

  set +o errexit
  set +o nounset
  set +o pipefail
}

teardown() {
  unset wget
  rm "${BATS_TMPDIR}/wget"

  unset doguctl
  rm "${BATS_TMPDIR}/doguctl"
}

teardown_file() {
  unset NODE_MASTER_FILE
  rm "${BATS_TMPDIR}/jq"
}

@test 'migrateServiceAccountsToFolders() should skip if request exit code is 8 and error message contains "404 Not Found"' {
  mock_set_status "${wget}" 8
  mock_set_side_effect "${wget}" 'echo "something something 404 Not Found..." >&2'

  run migrateServiceAccountsToFolders

  assert_success
  assert_equal "$(mock_get_call_num "${wget}")" '2'

  assert_equal "$(mock_get_call_args "${wget}" 1)" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oidc?recursive=false'
  assert_line "Service account type 'oidc' not found, skipping..."

  assert_equal "$(mock_get_call_args "${wget}" 2)" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oauth?recursive=false'
  assert_line "Service account type 'oauth' not found, skipping..."
}

@test 'migrateServiceAccountsToFolders() should fail if request exit code is neither 0 nor 8 and error message contains "404 Not Found"' {
  mock_set_status "${wget}" 7
  mock_set_side_effect "${wget}" 'echo "something something 404 Not Found..." >&2'

  run migrateServiceAccountsToFolders

  [ "${status}" -eq 7 ]
  assert_equal "$(mock_get_call_num "${wget}")" '1'
  assert_equal "$(mock_get_call_args "${wget}")" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oidc?recursive=false'
  assert_line "Failed to list service accounts of type 'oidc'"
  assert_line "something something 404 Not Found..."
}

@test 'migrateServiceAccountsToFolders() should fail if request exit code is not 0 and error message does not contain "404 Not Found"' {
  mock_set_status "${wget}" 8

  run migrateServiceAccountsToFolders

  [ "${status}" -eq 8 ]
  assert_equal "$(mock_get_call_num "${wget}")" '1'
  assert_equal "$(mock_get_call_args "${wget}")" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oidc?recursive=false'
  assert_line "Failed to list service accounts of type 'oidc'"
}

@test 'migrateServiceAccountsToFolders() should succeed' {
  mock_set_output "${wget}" "$(<'/workspace/batsTests/oidc-sa-response.json')" '1'
  mock_set_output "${wget}" "$(<'/workspace/batsTests/oauth-sa-response.json')" '2'

  run migrateServiceAccountsToFolders

  assert_success
  assert_equal "$(mock_get_call_num "${wget}")" '2'
  assert_equal "$(mock_get_call_num "${doguctl}")" '8'

  assert_line "Migrating service accounts of type 'oidc'..."
  assert_equal "$(mock_get_call_args "${wget}" '1')" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oidc?recursive=false'
  assert_line "Migrating service account directory for 'teamscale'"
  assert_equal "$(mock_get_call_args "${doguctl}" '1')" 'config --remove service_accounts/oidc/teamscale'
  assert_equal "$(mock_get_call_args "${doguctl}" '2')" 'config service_accounts/oidc/teamscale/secret aHR0cHM6Ly93d3cueW91dHViZS5jb20vd2F0Y2g/dj1kUXc0dzlXZ1hjUQo='
  assert_line "Migrating service account directory for 'openproject'"
  assert_equal "$(mock_get_call_args "${doguctl}" '3')" 'config --remove service_accounts/oidc/openproject'
  assert_equal "$(mock_get_call_args "${doguctl}" '4')" 'config service_accounts/oidc/openproject/secret TmV2ZXIgZ29ubmEgZ2l2ZSB5b3UgdXAuLi4K'
  assert_line "Migrating service accounts of type 'oidc'... Done!"

  assert_line "Migrating service accounts of type 'oauth'..."
  assert_equal "$(mock_get_call_args "${wget}" '2')" '-O- http://192.168.56.2:4001/v2/keys/config/cas/service_accounts/oauth?recursive=false'
  assert_line "Migrating service account directory for 'portainer'"
  assert_equal "$(mock_get_call_args "${doguctl}" '5')" 'config --remove service_accounts/oauth/portainer'
  assert_equal "$(mock_get_call_args "${doguctl}" '6')" 'config service_accounts/oauth/portainer/secret aHR0cHM6Ly93d3cueW91dHViZS5jb20vd2F0Y2g/dj1kalYxMVhiYzkxNAo='
  assert_line "Migrating service account directory for 'another_oauth_dogu'"
  assert_equal "$(mock_get_call_args "${doguctl}" '7')" 'config --remove service_accounts/oauth/another_oauth_dogu'
  assert_equal "$(mock_get_call_args "${doguctl}" '8')" 'config service_accounts/oauth/another_oauth_dogu/secret VGFrZSBtZSBvbi4uLgo='
  assert_line "Migrating service accounts of type 'oauth'... Done!"
}

@test 'migrateLogoutUri() should succeed' {
  mock_set_side_effect "${wget}" 'cat /workspace/batsTests/dogu-logoutUri-response.json'

  run migrateLogoutUri

  assert_success
  assert_equal "$(mock_get_call_num "${wget}")" '1'
  assert_equal "$(mock_get_call_args "${wget}")" '-O- http://192.168.56.2:4001/v2/keys/dogu?recursive=true'

  assert_equal "$(mock_get_call_num "${doguctl}")" '2'
  assert_line "Migrating logout URI for dogu 'teamscale'"
  assert_equal "$(mock_get_call_args "${doguctl}" '1')" 'config service_accounts/oidc/teamscale/logout_uri /api/auth/openid/logout'
  assert_line "Migrating logout URI for dogu 'grafana'"
  assert_equal "$(mock_get_call_args "${doguctl}" '2')" 'config service_accounts/cas/grafana/logout_uri /api/auth/oauth/logout'
}