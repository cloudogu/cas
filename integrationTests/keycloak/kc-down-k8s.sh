#!/usr/bin/env bash
set -euo pipefail

# No-op for Kubernetes; optionally delete Keycloak pod or deployment.
# Usage: KC_NAMESPACE=ecosystem ./kc-down-k8s.sh [--delete-deployment]

NAMESPACE=${KC_NAMESPACE:-ecosystem}
DELETE_DEPLOYMENT=${1:-}

if [ "$DELETE_DEPLOYMENT" = "--delete-deployment" ]; then
  echo "Deleting Keycloak deployment in namespace ${NAMESPACE}"
  kubectl -n "${NAMESPACE}" delete deployment keycloak --ignore-not-found
  kubectl -n "${NAMESPACE}" delete sts keycloak --ignore-not-found || true
  exit 0
fi

echo "kc-down-k8s: nothing to do for Kubernetes (use --delete-deployment to remove keycloak)"

