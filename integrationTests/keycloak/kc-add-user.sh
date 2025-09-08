#!/usr/bin/env bash
set -euo pipefail

NAME="${NAME:-kc}"       # container name
REALM="${REALM:-Test}"
GROUP="${GROUP:-testers}"
USERNAME="${USERNAME:-tester}"
EMAIL="test@example.com"
PASSWORD="test"
FIRST="Test"
LAST="User"

usage() {
  cat <<'USAGE' >&2
Usage: kc-add-user.sh [-n container] [-r realm] [-g group] -u username -e email -p password [-f first] [-l last]
  -n  Keycloak container name (default: kc)
  -r  Realm (default: Test)
  -g  Group to ensure & assign (default: testers)
  -u  Username (required)
  -e  Email (required)
  -p  Password (required)
  -f  First name (default: Test)
  -l  Last name (default: User)
USAGE
  exit 1
}

while getopts ":n:r:g:u:e:p:f:l:" opt; do
  case "$opt" in
    n) NAME="$OPTARG" ;;
    r) REALM="$OPTARG" ;;
    g) GROUP="$OPTARG" ;;
    u) USERNAME="$OPTARG" ;;
    e) EMAIL="$OPTARG" ;;
    p) PASSWORD="$OPTARG" ;;
    f) FIRST="$OPTARG" ;;
    l) LAST="$OPTARG" ;;
    *) usage ;;
  esac
done

[ -z "$USERNAME" ] && usage
[ -z "$EMAIL" ]    && usage
[ -z "$PASSWORD" ] && usage

if ! sudo docker ps --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "[kc-add-user] Container '$NAME' not running. Start it first." >&2
  exit 1
fi

# create inside script (no host-side expansion)
TMP="$(mktemp)"
cat >"$TMP" <<'INSIDE'
#!/usr/bin/env bash
set -euo pipefail

# Keycloak 24 (Quarkus) usually runs at /auth; adjust if needed.
REL="/auth"
BASE="http://localhost:8080${REL}"

# Find kcadm for legacy/quarkus layouts
if [ -x /opt/jboss/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/jboss/keycloak/bin/kcadm.sh"
elif [ -x /opt/keycloak/bin/kcadm.sh ]; then
  KCADM="/opt/keycloak/bin/kcadm.sh"
else
  echo "[inside] kcadm.sh not found" >&2
  exit 1
fi

: "${REALM:?missing REALM}"
: "${GROUP:?missing GROUP}"
: "${USERNAME:?missing USERNAME}"
: "${EMAIL:?missing EMAIL}"
: "${PASSWORD:?missing PASSWORD}"
: "${FIRST:?missing FIRST}"
: "${LAST:?missing LAST}"

echo "[inside] Login to master…"
"$KCADM" config credentials --server "$BASE" --realm master --user admin --password admin

# Ensure realm
"$KCADM" get "realms/${REALM}" >/dev/null 2>&1 || {
  echo "[inside] Creating realm ${REALM}…"
  "$KCADM" create realms -s realm="${REALM}" -s enabled=true
}

# Ensure group
if ! "$KCADM" get groups -r "${REALM}" | tr -d '\r' | grep -q "\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\""; then
  echo "[inside] Creating group ${GROUP}…"
  "$KCADM" create groups -r "${REALM}" -s name="${GROUP}"
fi

# Resolve group ID (no awk/jq)
GROUP_ID=""
GROUP_JSON_SEARCH="$("$KCADM" get "groups?search=${GROUP}" -r "${REALM}" 2>/dev/null | tr -d '\r\n')" || true
if [ -n "${GROUP_JSON_SEARCH}" ]; then
  # try id-after-name
  GROUP_ID="$(printf '%s' "$GROUP_JSON_SEARCH" \
    | sed -n "s/.*\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\"[^\"]*\"id\"[[:space:]]*:[[:space:]]*\"\([^\"]\+\)\".*/\1/p" \
    | head -n1)"
  # try id-before-name
  [ -z "$GROUP_ID" ] && GROUP_ID="$(printf '%s' "$GROUP_JSON_SEARCH" \
    | sed -n "s/.*\"id\"[[:space:]]*:[[:space:]]*\"\([^\"]\+\)\"[^\"]*\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\".*/\1/p" \
    | head -n1)"
fi
if [ -z "$GROUP_ID" ]; then
  GROUP_JSON_ALL="$("$KCADM" get "groups?briefRepresentation=true&max=500" -r "${REALM}" 2>/dev/null | tr -d '\r\n')" || true
  [ -n "$GROUP_JSON_ALL" ] && GROUP_ID="$(printf '%s' "$GROUP_JSON_ALL" \
    | sed -n "s/.*\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\"[^\"]*\"id\"[[:space:]]*:[[:space:]]*\"\([^\"]\+\)\".*/\1/p" \
    | head -n1)"
fi
if [ -z "$GROUP_ID" ]; then
  echo "[inside][ERROR] Could not resolve group id for '${GROUP}'" >&2
  exit 1
fi
echo "[inside] Group ${GROUP} id: ${GROUP_ID}"

# Ensure user
if ! "$KCADM" get users -r "${REALM}" -q username="${USERNAME}" --fields id | grep -q '"id"'; then
  echo "[inside] Creating user ${USERNAME}…"
  "$KCADM" create users -r "${REALM}" \
    -s username="${USERNAME}" -s enabled=true \
    -s email="${EMAIL}" -s emailVerified=true \
    -s firstName="${FIRST}" -s lastName="${LAST}"
fi

# Resolve user ID
USER_ID="$(
  "$KCADM" get users -r "${REALM}" -q username="${USERNAME}" --fields id 2>/dev/null \
  | tr -d '\r\n' \
  | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]\+\)".*/\1/p' \
  | head -n1
)"
[ -z "$USER_ID" ] && { echo "[inside][ERROR] Could not resolve user id for '${USERNAME}'" >&2; exit 1; }
echo "[inside] User ${USERNAME} id: ${USER_ID}"

# Set password
"$KCADM" set-password -r "${REALM}" --userid "${USER_ID}" --new-password "${PASSWORD}" --temporary=false || true

# Add to group if not already a member
if ! "$KCADM" get "users/${USER_ID}/groups" -r "${REALM}" | tr -d '\r' \
     | grep -q "\"name\"[[:space:]]*:[[:space:]]*\"${GROUP}\""; then
  echo "[inside] Adding ${USERNAME} to ${GROUP}…"
  "$KCADM" update "users/${USER_ID}/groups/${GROUP_ID}" -r "${REALM}" -n
else
  echo "[inside] ${USERNAME} already in ${GROUP}."
fi

echo "[inside] Done."
INSIDE

chmod -R 777 "$TMP"
sudo docker cp "$TMP" "$NAME":/tmp/kc-add-user.inside.sh
rm -f "$TMP"
sudo docker exec -i \
  -e REALM="$REALM" \
  -e GROUP="$GROUP" \
  -e USERNAME="$USERNAME" \
  -e EMAIL="$EMAIL" \
  -e PASSWORD="$PASSWORD" \
  -e FIRST="$FIRST" \
  -e LAST="$LAST" \
  "$NAME" bash /tmp/kc-add-user.inside.sh

echo "[kc-add-user] Success."
