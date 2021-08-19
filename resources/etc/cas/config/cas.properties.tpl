########################################################################################################################
# General server configuration
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#cas-server
# ----------------------------------------------------------------------------------------------------------------------
cas.server.name=https://{{ .GlobalConfig.Get "fqdn" }}
cas.server.prefix=${cas.server.name}/cas
ces.services.stage={{ .GlobalConfig.GetOrDefault "stage" "production" }}
# Unique CAS node name
# host.name is used to generate unique Service Ticket IDs and SAMLArtifacts.  This is usually set to the specific
# hostname of the machine running the CAS node, but it could be any label so long as it is unique in the cluster.
host.name=cas.{{ .GlobalConfig.Get "domain" }}
########################################################################################################################

########################################################################################################################
# Logging configuration
# Configuration guide: https://apereo.github.io/cas/6.3.x/logging/Logging.html
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#logging
# ----------------------------------------------------------------------------------------------------------------------
logging.config=file:/etc/cas/config/log4j2.xml
########################################################################################################################

########################################################################################################################
# LDAP
# Configuration guide: https://apereo.github.io/cas/6.3.x/installation/LDAP-Authentication.html
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#ldap-authentication
# ----------------------------------------------------------------------------------------------------------------------
# Configure ldap-mapper as ldap source service
cas.authn.ldap[0].ldap-url=ldap://ldap-mapper:3893/
cas.authn.ldap[0].base-dn=ou=People,dc=cloudogu,dc=com
cas.authn.ldap[0].type=DIRECT
cas.authn.ldap[0].dn-format=uid=%s,ou=People,dc=cloudogu,dc=com
cas.authn.ldap[0].search-filter=(&(objectClass=person)(uid={user}))
cas.authn.ldap[0].principal-attribute-list=uid:username,cn,mail,givenName,sn:surname,displayName,memberOf:groups
cas.authn.ldap[0].disable-pooling=true
ces.services.allowedAttributes=username,cn,mail,givenName,surname,displayName,groups
# Disable static users
cas.authn.accept.enabled=false
########################################################################################################################

########################################################################################################################
# Throttling
# Configuration guide: https://apereo.github.io/cas/6.3.x/installation/Configuring-Authentication-Throttling.html
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#authentication-throttling
# ----------------------------------------------------------------------------------------------------------------------
{{ if ne (.Config.GetOrDefault "limit/max_number" "0") "0" }}
    # Authentication Failure Throttling
    cas.authn.throttle.username-parameter=username
    cas.authn.throttle.app-code=CAS
    cas.authn.throttle.failure.code=AUTHENTICATION_FAILED
    cas.authn.throttle.failure.max_number={{ .Config.GetOrDefault "limit/max_number" "0"}}
    cas.authn.throttle.failure.failure_store_time={{ .Config.GetOrDefault "limit/failure_store_time" "0"}}
    cas.authn.throttle.failure.lockTime={{ .Config.GetOrDefault "limit/lock_time" "0"}}
{{ end }}
########################################################################################################################

########################################################################################################################
# Timeouts
# Configuration guide: https://apereo.github.io/cas/6.3.x/ticketing/Configuring-Ticket-Expiration-Policy.html#timeout
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#default
# ----------------------------------------------------------------------------------------------------------------------
cas.ticket.tgt.max-time-to-live-in-seconds={{ .Config.GetOrDefault "session_tgt/max_time_to_live_in_seconds" "86400"}}
cas.ticket.tgt.time-to-kill-in-seconds={{ .Config.GetOrDefault "session_tgt/time_to_kill_in_seconds" "36000"}}
########################################################################################################################

########################################################################################################################
# OAuth
# Configuration guide: https://apereo.github.io/cas/6.3.x/installation/OAuth-OpenId-Authentication.html
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#oauth2
# ----------------------------------------------------------------------------------------------------------------------
# Time for the code to expire
cas.authn.oauth.code.timeToKillInSeconds=30
cas.authn.oauth.code.numberOfUses=1
# Access Token (Session) is valid for 1 day (= 86000 seconds)
cas.authn.oauth.accessToken.timeToKillInSeconds=86000
cas.authn.oauth.accessToken.maxTimeToLiveInSeconds=86000
########################################################################################################################

########################################################################################################################
# OIDC
# Configuration guide:
# Properties: https://apereo.github.io/cas/6.1.x/configuration/Configuration-Properties-Common.html#delegated-authentication-openid-connect-settings
########################################################################################################################
### spezifisch für keycloak
### wird für die Ermittlung der Metadaten verwendet
### cas.authn.pac4j.oidc[0].keycloak.base-uri=https://staging-account.cloudogu.com/auth
### cas.authn.pac4j.oidc[0].keycloak.realm=Cloudogu
### allgemeine Alternative
cas.authn.pac4j.oidc[0].generic.discovery-uri=https://staging-account.cloudogu.com/auth/realms/Cloudogu/.well-known/openid-configuration

### useNonce muss für die Anbindung an unseren Keycloak konfiguriert werden
cas.authn.pac4j.oidc[0].generic.useNonce=true
cas.authn.pac4j.oidc[0].generic.enabled=true

### Name und Secret des im Keycloak angelegten Clients
cas.authn.pac4j.oidc[0].generic.id=my-client-id
cas.authn.pac4j.oidc[0].generic.secret=98199dd4-17ca-4021-8987-fc8ade8d685d

### Der Client-Name wird zwar immer wieder bei den Requests angegeben, ist aber eine optionale Information
cas.authn.pac4j.oidc[0].generic.client-name=my-client-name

### automatische Umleitung zum konfigurierten Provider, wenn man sich ..ber CAS einloggen m..chte
cas.authn.pac4j.oidc[0].generic.auto-redirect=true

### Informationen, die in der Antwort vom OIDC-Provider enthalten sein sollen
cas.authn.pac4j.oidc[0].generic.scope=openid email profile
cas.authn.pac4j.oidc[0].generic.responseType=code

### Attribute mapping
cas.services.attributeMapping=email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName