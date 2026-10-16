# KeyCloak für OIDC Testumgebung aufsetzen

Um die OIDC Anmeldung via KeyCloak zu testen, kann ein lokaler KeyCloak aufgesetzt und konfiguriert werden.

## Installieren

Dieses YAML installiert einen Keycloak und konfiguriert bereits einige Demouser.

Um das Yaml im eigenen Cluster nutzen zu können, muss der wert von `<fqdn>` entsprechend angepasst werden.

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-admin
type: Opaque
stringData:
  KC_BOOTSTRAP_ADMIN_USERNAME: admin
  KC_BOOTSTRAP_ADMIN_PASSWORD: change-me-admin
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-realm
data:
  Cloudogu-realm.json: |
    {
      "realm": "Cloudogu",
      "enabled": true,
      "sslRequired": "none",
      "scimApiEnabled": true,
      "registrationAllowed": false,
      "loginWithEmailAllowed": true,
      "groups": [
        {
          "id": "allowed-group",
          "name": "allowed-group"
        }
      ],
      "clients": [
        {
          "clientId": "casClient",
          "name": "CAS",
          "enabled": true,
          "protocol": "openid-connect",
          "publicClient": false,
          "secret": "cas-client-secret",
          "standardFlowEnabled": true,
          "directAccessGrantsEnabled": true,
          "serviceAccountsEnabled": true,
          "redirectUris": [
            "http://<fqdn>/cas/*",
            "https://<fqdn>/cas/*"
          ],
          "webOrigins": [
            "http://<fqdn>"
          ],
          "protocolMappers": [
            {
              "name": "groups",
              "protocol": "openid-connect",
              "protocolMapper": "oidc-group-membership-mapper",
              "config": {
                "full.path": "false",
                "id.token.claim": "true",
                "access.token.claim": "true",
                "userinfo.token.claim": "true",
                "claim.name": "groups",
                "jsonType.label": "String"
              }
            }
          ]
        }
      ],
      "users": [
        {
          "username": "testi",
          "enabled": true,
          "email": "testi@example.com",
          "emailVerified": true,
          "firstName": "Test",
          "lastName": "User",
          "groups": [
            "/allowed-group"
          ],
          "credentials": [
            {
              "type": "password",
              "value": "testi",
              "temporary": false
            }
          ]
        },
        {
          "username": "service-account-casClient",
          "enabled": true,
          "serviceAccountClientId": "casClient",
          "clientRoles": {
            "realm-management": [
              "manage-users",
              "view-realm"
            ]
          }
        }
      ]
    }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:26.6.0
          args:
            - start-dev
            - --features=scim-api
            - --import-realm
          env:
            - name: KC_BOOTSTRAP_ADMIN_USERNAME
              valueFrom:
                secretKeyRef:
                  name: keycloak-admin
                  key: KC_BOOTSTRAP_ADMIN_USERNAME
            - name: KC_BOOTSTRAP_ADMIN_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-admin
                  key: KC_BOOTSTRAP_ADMIN_PASSWORD
            - name: KC_HTTP_ENABLED
              value: "true"
            - name: KC_HEALTH_ENABLED
              value: "true"
            - name: KC_HOSTNAME
              value: "http://<fqdn>/keycloak"
            - name: KC_HTTP_RELATIVE_PATH
              value: /keycloak
            - name: KC_HOSTNAME_STRICT
              value: "false"
          ports:
            - name: http
              containerPort: 8080
            - name: management
              containerPort: 9000
          readinessProbe:
            httpGet:
              path: /keycloak/health/ready
              port: management
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /keycloak/health/live
              port: management
            initialDelaySeconds: 30
            periodSeconds: 15
          volumeMounts:
            - name: realm
              mountPath: /opt/keycloak/data/import
              readOnly: true
      volumes:
        - name: realm
          configMap:
            name: keycloak-realm
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
spec:
  selector:
    app: keycloak
  ports:
    - name: http
      port: 8080
      targetPort: http
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: keycloak
spec:
  ingressClassName: k8s-ecosystem-ces-service
  rules:
    - http:
        paths:
          - path: /keycloak
            pathType: Prefix
            backend:
              service:
                name: keycloak
                port:
                  name: http
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: keycloak-ingress
  namespace: ecosystem
spec:
  podSelector:
    matchLabels:
      app: keycloak
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: ecosystem
      ports:
        - protocol: TCP
          port: 8080

```

### Demouser:

- Username: admin
    - pw: change-me-admin
- Username: testi
    - pw: testi

## CAS-Dogu konfigurieren

Nach der Installation des KeyCloak muss dieser in der  CAS-Dogu-Configmap konfiguriert werden.

```yaml
oidc:
  enabled: "true"
  discovery_uri: "http://<fqdn>/keycloak/realms/Cloudogu/.well-known/openid-configuration"
  client_id: "casClient"
  client_secret: "cas-client-secret"
  display_name: "Keycloak"
  optional: "true"
  scopes: "openid email profile groups"
  principal_attribute: "preferred_username"
  attribute_mapping: "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName,groups:externalGroups"
  allowed_groups: "allowed-group"
  initial_admin_usernames: "testi"
  ldap_sync:
    interval: "PT1M"
    timeout: "PT5M"
    authentication_method: "bearer"
    token_uri: "http://<fqdn>/keycloak/realms/Cloudogu/protocol/openid-connect/token"
    scim_base_url: "http://<fqdn>/keycloak/realms/Cloudogu/scim/v2"
    scim_page_size: "100"
    ldap_identity_attribute: "uid"
    ldap_external_attribute: "external"
    ldap_external_value: "TRUE"
```

ausserdem muss das KeyCloak-Secret auch in der CAS-Secret-Map hinterlegt sein
