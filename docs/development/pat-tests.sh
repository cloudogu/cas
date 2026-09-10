#!/usr/bin/env bash
set -euo pipefail

# Bei Bedarf über Umgebungsvariablen überschreiben.
CAS_URL="${CAS_URL:-https://34.185.221.93/cas}"
SA_USER="${SA_USER:-pat-api}"
SA_PASSWORD="${SA_PASSWORD:-securePassword}"
USER_ID="${USER_ID:-mbergen}"
PAT_ID="${PAT_ID:-}"
SCOPE="/redmine,/usermgt"
DISPLAY_NAME="PAT API Test"
EXPIRES_AT=""

usage() {
    cat <<'EOF'
Aufruf: ./pat-tests.sh <Befehl> [Optionen]

  create          PAT erstellen
                  --scope <Scope>       Standard: /redmine,/usermgt
                  --displayname <Name>  Standard: PAT API Test
                  --expires-at <Datum>  Optional: ISO-8601-Zeitpunkt in der Zukunft
                                        z. B. 2027-12-31T23:59:59Z (UTC)
                                        Ohne Angabe: kein Ablaufdatum
  list            Alle PATs auflisten
  get <PAT-ID>    Einzelnes PAT abrufen
  invalid-auth    Ungültige Authentifizierung prüfen (erwartet: 401 Unauthorized)
  delete <PAT-ID> PAT löschen

Alternativ zur PAT-ID als Argument kann die Umgebungsvariable PAT_ID gesetzt werden.

Beispiel:
  ./pat-tests.sh create --scope /usermgt --displayname "PAT Usermgt only" --expires-at "2027-12-31T23:59:59Z"
EOF
}

require_pat_id() {
    if [[ -z "$PAT_ID" ]]; then
        echo "Fehler: Für diesen Schritt muss eine PAT-ID angegeben werden." >&2
        exit 1
    fi
}

create_pat() {
    local payload
    payload=$(jq -n --arg displayName "$DISPLAY_NAME" --arg scope "$SCOPE" \
        --arg expiresAt "$EXPIRES_AT" \
        '{displayName: $displayName, scope: $scope}
         + (if $expiresAt == "" then {} else {expiresAt: $expiresAt} end)')
    curl -k -i \
    --user "${SA_USER}:${SA_PASSWORD}" \
    -H "Content-Type: application/json" \
    -X POST \
    "${CAS_URL}/api/users/${USER_ID}/pats" \
    --data "$payload"
}

list_pats() {
    curl -k --fail-with-body --silent --show-error \
    --user "${SA_USER}:${SA_PASSWORD}" \
    "${CAS_URL}/api/users/${USER_ID}/pats" | jq
}

get_pat() {
    require_pat_id
    curl -k --fail-with-body --silent --show-error \
    --user "${SA_USER}:${SA_PASSWORD}" \
    "${CAS_URL}/api/users/${USER_ID}/pats/${PAT_ID}" | jq
}

check_invalid_auth() {
    # Erwartet: 401 Unauthorized.
    curl -k -i \
    --user "admin:falsches-password" \
    "${CAS_URL}/api/users/${USER_ID}/pats"
}

delete_pat() {
    require_pat_id
    curl -k -i \
    --user "${SA_USER}:${SA_PASSWORD}" \
    -X DELETE \
    "${CAS_URL}/api/users/${USER_ID}/pats/${PAT_ID}"
}

fail() {
    echo "Fehler: $*" >&2
    exit 1
}

command="${1:-}"
if (( $# > 0 )); then
    shift
fi

case "$command" in
    create)
        while (( $# > 0 )); do
            case "$1" in
                --scope|--displayname|--expires-at)
                    option="$1"
                    [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || fail "Wert für $option fehlt."
                    case "$option" in
                        --scope) SCOPE="$2" ;;
                        --displayname) DISPLAY_NAME="$2" ;;
                        --expires-at) EXPIRES_AT="$2" ;;
                    esac
                    shift 2
                    ;;
                -h|--help) usage; exit 0 ;;
                *) fail "Unbekannte Option für create: $1" ;;
            esac
        done
        create_pat
        ;;
    get|delete)
        (( $# <= 1 )) || fail "Für $command ist nur eine PAT-ID erlaubt."
        if (( $# == 1 )); then
            case "$1" in
                -h|--help) usage; exit 0 ;;
                -*) fail "Unbekannte Option für $command: $1" ;;
            esac
            PAT_ID="$1"
        fi
        case "$command" in
            get) get_pat ;;
            delete) delete_pat ;;
        esac
        ;;
    list|invalid-auth)
        (( $# == 0 )) || fail "Für $command sind keine Argumente erlaubt."
        case "$command" in
            list) list_pats ;;
            invalid-auth) check_invalid_auth ;;
        esac
        ;;
    ""|-h|--help) usage ;;
    *) fail "Unbekannter Befehl '$command'. Siehe --help." ;;
esac
