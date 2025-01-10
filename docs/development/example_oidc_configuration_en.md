# Example configuration for delegated authentication with OIDC

This is an example configuration for delegated authentication in CAS via OIDC.
These values can be entered directly in the `cas-config`-ConfigMap in CES-MN.
For Classic-CES, they must be entered accordingly in the ETCD.

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

**Attention:** In production mode, the config value for `client_secret` should be entered in the Kubernetes-Secret of the CAS-Config.