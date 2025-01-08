# Beispiel-Konfiguration für delegierte Authentifizierung mit OIDC

Dies ist eine Beispiel-Konfiguration für die delegierte Authentifizierung im CAS über OIDC.
Diese Werte können im CES-MN direkt in die `cas-config`-ConfigMap eingetragen werden.
Für Classic-CES müssen sie entsprechend im ETCD eingetragen werden.

```yaml
oidc:
  enabled: "true"
  discovery_uri: "http://192.168.56.1:8080/auth/realms/Cloudogu/.well-known/openid-configuration"
  client_id: "cesCasClient"
  client_secret: "MySecretSecret"
  #redirect_uri: "https://platform.cloudogu.com/de/"                                                                                                                                                             
  display_name: "Cloudogu-Platform"
  optional: "true"
  scopes: "openid email profile GroupScope"
  principal_attribute: "preferred_username"
  attribute_mapping: "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName,groups:externalGroups"
  allowed_groups: "Gruppe2, Gruppe3"
  initial_admin_usernames: "user1, testAdmin"                      
```

> **Achtung:** Im Produktions-Betrieb sollte der Config-Wert für `client_secret` im Kubernetes-Secret der CAS-Config eingetragen werden. 