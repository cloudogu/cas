#!/usr/bin/env bash
set -euo pipefail

# Wait for a Keycloak pod in Kubernetes and perform basic setup inside the pod.
# Usage: KC_NAMESPACE=ecosystem ADMIN_USER=admin ADMIN_PASS=admin ./kc-up-k8s.sh

NAMESPACE=${KC_NAMESPACE:-ecosystem}
ADMIN_USER=${ADMIN_USER:-admin}
ADMIN_PASS=${ADMIN_PASS:-admin}
TIMEOUT=${TIMEOUT:-600}

log(){ printf '[kc-up-k8s] %s\n' "$*" >&2; }

log "Waiting for a Keycloak pod in namespace '${NAMESPACE}'..."
for i in $(seq 1 $((TIMEOUT/5))); do
  POD=$(kubectl -n "$NAMESPACE" get pod -l app.kubernetes.io/name=keycloak -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
  if [ -n "$POD" ]; then
    log "Found pod: $POD"
    break
  fi
  sleep 5
done

if [ -z "${POD:-}" ]; then
  log "No Keycloak pod found in namespace ${NAMESPACE}."
  exit 1
fi

log "Waiting for Keycloak discovery endpoint to respond inside the pod..."
DISCOVERY='http://localhost:8080/auth/realms/master/.well-known/openid-configuration'
for i in $(seq 1 $((TIMEOUT/5))); do
  if kubectl -n "$NAMESPACE" exec "$POD" -- curl -fsS "$DISCOVERY" >/dev/null 2>&1; then
    log "Keycloak is up inside pod $POD"
    break
  fi
  sleep 5
done

if ! kubectl -n "$NAMESPACE" exec "$POD" -- curl -fsS "$DISCOVERY" >/dev/null 2>&1; then
  log "Keycloak discovery endpoint did not become ready in time"
  kubectl -n "$NAMESPACE" logs "$POD" --tail=200 || true
  exit 1
fi

log "Logging into Keycloak (kcadm) inside the pod"
kubectl -n "$NAMESPACE" exec "$POD" -- /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080/auth --realm master --user "$ADMIN_USER" --password "$ADMIN_PASS"

log "Disabling ssl requirement for realm 'master' inside pod"
kubectl -n "$NAMESPACE" exec "$POD" -- /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE || true

log "Done. You can now run the other kc-*-k8s.sh scripts to perform realm/client/user setup inside the pod."

