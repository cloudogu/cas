#!/usr/bin/env sh
# Prune duplicate JARs in a exploded WAR by keeping only the highest version per artifact.
# Works with filenames like: artifactId-1.2.3.jar or artifactId-1.2.3-classifier.jar.
# Usage:
#   prune-jars.sh [--dry-run] [LIB_DIR]
# Default LIB_DIR = ./WEB-INF/lib

set -eu

DRYRUN=0
if [ "${1:-}" = "--dry-run" ]; then
  DRYRUN=1
  shift
fi

LIB_DIR="${1:-./WEB-INF/lib}"

if [ ! -d "$LIB_DIR" ]; then
  echo "LIB_DIR not found: $LIB_DIR" >&2
  exit 1
fi

cd "$LIB_DIR"

# 1) Build a list of candidate jars: artifactId + version + full name
# We consider any *.jar that has a -<version> before .jar (with optional classifier).
# Regex-ish parsing done with POSIX tools.
# Output format: "base|version|filename"
list_candidates() {
  # shellcheck disable=SC2010
  ls -1 *.jar 2>/dev/null | while read -r j; do
    # strip ".jar"
    noext=${j%.jar}
    # find last '-' which should precede the version token
    base=${noext%-*}
    vers=${noext##*-}
    # If there is still a '-' inside vers (classifier), split on first '-' from right
    # Try to detect versions by starting with a digit
    case "$vers" in
      [0-9]*)
        # looks like a version at the end -> ok
        printf '%s|%s|%s\n' "$base" "$vers" "$j"
        ;;
      *)
        # maybe we have "...-<version>-<classifier>"
        # take the last dash segment as classifier; version is the segment before
        pre=${noext%*-*}
        maybe_ver=${pre##*-}
        case "$maybe_ver" in
          [0-9]*)
            printf '%s|%s|%s\n' "${pre%-*}" "$maybe_ver" "$j"
            ;;
          *)
            # not in expected pattern, skip
            ;;
        esac
        ;;
    esac
  done
}

# Version compare in POSIX AWK:
# split by [.-], compare numerically when possible, otherwise lexicographically
# returns highest version per base
choose_highest() {
  awk -F'|' '
    function splitver(v, arr,    n,i,tok) {
      n = split(v, arr, /[.-]/)
      # normalize: turn numeric tokens into numbers, keep strings
      for (i=1;i<=n;i++) {
        if (arr[i] ~ /^[0-9]+$/) arr[i]+=0
      }
      return n
    }
    function vercmp(a,b,    A,B,na,nb,i) {
      na = splitver(a, A)
      nb = splitver(b, B)
      # compare token by token
      for (i=1; i<=na || i<=nb; i++) {
        va = (i in A)?A[i]:""
        vb = (i in B)?B[i]:""
        # missing tokens sort lower
        if (va=="" && vb!="") return -1
        if (vb=="" && va!="") return  1
        # numeric vs numeric
        if (va ~ /^[0-9]+$/ && vb ~ /^[0-9]+$/) {
          if (va+0 < vb+0) return -1
          if (va+0 > vb+0) return  1
        } else {
          # string compare
          if (va < vb) return -1
          if (va > vb) return  1
        }
      }
      return 0
    }
    {
      base=$1; ver=$2; file=$3
      if (!(base in bestver) || vercmp(ver, bestver[base]) > 0) {
        bestver[base]=ver
        bestfile[base]=file
      }
      all[base, ver]=file
    }
    END {
      for (k in bestver) {
        printf("KEEP|%s|%s|%s\n", k, bestver[k], bestfile[k])
      }
      for (kv in all) {
        split(kv, parts, SUBSEP)
        b=parts[1]; v=parts[2]
        if (v != bestver[b]) {
          printf("DROP|%s|%s|%s\n", b, v, all[kv])
        }
      }
    }
  '
}

plan="$(list_candidates | choose_highest)"

# keep a tiny safety: never delete when base starts with e.g. "jakarta.servlet" etc.
ALLOW_BASE_PREFIXES=""

echo "$plan" | while IFS='|' read -r action base ver file; do
  [ -n "$action" ] || continue
  case "$action" in
    KEEP)
      # informative
      # echo "keep: $file"
      ;;
    DROP)
      # skip allowlist
      for p in $ALLOW_BASE_PREFIXES; do
        case "$base" in
          $p*) continue 2 ;;
        esac
      done
      if [ $DRYRUN -eq 1 ]; then
        echo "[dry-run] rm -f $file"
      else
        echo "rm -f $file"
        rm -f -- "$file"
      fi
      ;;
  esac
done

echo "Done pruning in $LIB_DIR"
