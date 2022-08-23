########################################################################################################################
# General server configuration
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#cas-server
# ----------------------------------------------------------------------------------------------------------------------
cas.server.name=https://{{ .GlobalConfig.Get "fqdn" }}
cas.server.prefix=${cas.server.name}/cas

ces.services.stage={{ .GlobalConfig.GetOrDefault "stage" "production" }}

# This property is very important. If this is not set to 0, the whole dogu can crash when ldap is not available
ces.ldap-pool-size=0

# Unique CAS node name
# host.name is used to generate unique Service Ticket IDs and SAMLArtifacts. This is usually set to the specific
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
# Email configuration
# https://apereo.github.io/cas/6.5.x/notifications/Sending-Email-Configuration.html
#------------------------------------------------------------------------------------------------------------------------
spring.mail.host=postfix
spring.mail.port=25
spring.mail.protocol=smtp
########################################################################################################################

########################################################################################################################
# LDAP
# Configuration guide: https://apereo.github.io/cas/6.3.x/installation/LDAP-Authentication.html
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#ldap-authentication
#             https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties-Common.html#ldap-connection-settings
#             https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties-Common.html#connection-strategies
#             https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties-Common.html#ldap-authenticationsearch-settings
#             https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties-Common.html#ldap-ssl-trust-managers
# ----------------------------------------------------------------------------------------------------------------------

#========================================
# General properties
#========================================
cas.authn.ldap[0].ldap-url={{ .Env.Get "LDAP_PROTOCOL" }}://{{ .Config.Get "ldap/host"}}:{{ .Config.Get "ldap/port"}}

# Manager DN for authenticated searches
cas.authn.ldap[0].bind-dn={{ .Env.Get "LDAP_BIND_DN" }}

# Manager password for authenticated searches
{{ if eq (.Config.Get "ldap/ds_type") "external"}}
cas.authn.ldap[0].bind-credential={{ .Config.GetAndDecrypt "ldap/password" }}
{{ end }}
{{ if eq (.Config.Get "ldap/ds_type") "embedded"}}
cas.authn.ldap[0].bind-credential={{ .Config.GetAndDecrypt "sa-ldap/password" }}
{{ end }}

# LDAP connection timeout in milliseconds
cas.authn.ldap[0].connect-timeout=3000

# Whether to use StartTLS
cas.authn.ldap[0].use-start-tls={{ .Env.Get "LDAP_STARTTLS" }}

cas.authn.ldap[0].principal-attribute-id={{ .Config.Get "ldap/attribute_id"}}
cas.authn.ldap[0].principal-attribute-list={{ .Config.Get "ldap/attribute_id"}}:username,cn,{{ .Config.Get "ldap/attribute_mail"}}:mail,{{ .Config.GetOrDefault "ldap/given_name" "givenName"}}:givenName,{{ .Config.GetOrDefault "ldap/surname" "sn"}}:surname,displayName,{{ .Config.Get "ldap/attribute_group"}}:groups
cas.authn.attributeRepository.ldap[0].attributes.groups={{ .Config.Get "ldap/attribute_group"}}
ces.services.allowedAttributes=username,cn,mail,givenName,surname,displayName,groups

#========================================
# LDAP connection pool configuration
#========================================
# This property is very important. If this is not set to 0, the whole dogu can crash when ldap is not available
cas.authn.ldap[0].min-pool-size=${ces.ldap-pool-size}
cas.authn.ldap[0].max-pool-size=10
cas.authn.ldap[0].validate-on-checkout=false
cas.authn.ldap[0].validate-periodically=true

# Amount of time in milliseconds to block on pool exhausted condition
# before giving up.
cas.authn.ldap[0].block-wait-time=3000

# Frequency of connection validation in seconds
# Only applies if validatePeriodically=true
cas.authn.ldap[0].validate-period=300

# Attempt to prune connections every N seconds
cas.authn.ldap[0].prune-period=300

# Maximum amount of time an idle connection is allowed to be in
# pool before it is liable to be removed/destroyed
cas.authn.ldap[0].idle-time=600

#========================================
# Authentication
#========================================
cas.authn.ldap[0].type=AUTHENTICATED

# Base DN of users to be authenticated
cas.authn.ldap[0].base-dn={{ .Env.Get "LDAP_BASE_DN" }}

# Search filter used for configurations that require searching for DNs
cas.authn.ldap[0].search-filter={{ .Env.Get "LDAP_SEARCH_FILTER" }}

# member search settings

# This property is very important. If this is not set to 0, the whole dogu can crash when ldap is not available
cas.authn.attributeRepository.ldap[0].min-pool-size=${ces.ldap-pool-size}

# settings for ldap group search by member
# base dn for group search e.g.: o=ces.local,dc=cloudogu,dc=com
cas.authn.attributeRepository.ldap[0].attributes.baseDn={{ .Config.GetOrDefault "ldap/group_base_dn" ""}}

# search filter for group search {0} will be replaced with the dn of the user
# e.g.: (member={0})
# if this property is empty, group search by member will be skipped
cas.authn.attributeRepository.ldap[0].attributes.searchFilter={{ .Config.GetOrDefault "ldap/group_search_filter" ""}}

# name attribute of groups e.g.: cn
cas.authn.attributeRepository.ldap[0].attributes.attribute.name={{ .Config.GetOrDefault "ldap/group_attribute_name" ""}}

# LDAP ssl trust manager: ANY | DEFAULT
cas.authn.ldap[0].trust-manager={{ .Env.Get "LDAP_TRUST_MANAGER" }}

# Enable (=true)/Disable (=false) static users
cas.authn.accept.enabled=false

# Disable LdapAuthenticationConfiguration-Bean to suppress registration of the LDAP Authentication handler of the cas.
# We use and register our own LDAP authentication handler by extending the LDAP authentication handler from the CAS.
# https://apereo.github.io/cas/6.3.x/configuration/Configuration-Management-Extensions.html#exclusions
spring.autoconfigure.exclude=org.apereo.cas.config.LdapAuthenticationConfiguration,org.apereo.cas.config.LdapPasswordManagementConfiguration

{{ if eq (.Config.Get "ldap/ds_type") "embedded"}}
#========================================
# Password management (pm)
# https://apereo.github.io/cas/6.5.x/password_management/Password-Management.html
#========================================
# General properties for password management
cas.authn.pm.core.enabled=true
cas.authn.pm.core.password-policy-pattern={{ .Env.Get "PASSWORD_POLICY_PATTERN" }}

# Properties for the connection to LDAP (required for changing the password of a user in LDAP)
cas.authn.pm.ldap[0].type=GENERIC

# This property is very important. If this is not set to 0, the whole dogu can crash when ldap is not available
cas.authn.pm.ldap[0].min-pool-size=${ces.ldap-pool-size}
cas.authn.pm.ldap[0].ldap-url=${cas.authn.ldap[0].ldap-url}
cas.authn.pm.ldap[0].base-dn=${cas.authn.ldap[0].base-dn}
cas.authn.pm.ldap[0].search-filter=${cas.authn.ldap[0].search-filter}

cas.authn.pm.ldap[0].bind-dn=${cas.authn.ldap[0].bind-dn}
cas.authn.pm.ldap[0].bind-credential=${cas.authn.ldap[0].bind-credential}

# Properties for sending an e-mail with a link to the CAS for changing the password
cas.authn.pm.reset.mail.attribute-name={{ .Config.Get "ldap/attribute_mail"}}
cas.authn.pm.reset.mail.from={{ .Config.GetOrDefault "mail_sender" "cas.dogu@cloudogu.com"}}
cas.authn.pm.reset.mail.subject={{ .Config.GetOrDefault "password_management/reset_password_subject" "Reset password"}}
cas.authn.pm.reset.mail.text={{ .Config.GetOrDefault "password_management/reset_password_text" "Hello!\\n\\nSomeone has just requested to change the login details of your CES account. If it was you, please follow the link below to reset them.\\n%s\\n\\nThis link will expire in 15 minutes.\\n\\nIf you do not wish to reset your credentials, simply ignore this message and nothing will change."}}
cas.authn.pm.reset.expiration=PT15M
cas.authn.pm.reset.security-questions-enabled=false
########################################################################################################################
{{ end }}

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
cas.ticket.tgt.primary.max-time-to-live-in-seconds={{ .Config.GetOrDefault "session_tgt/max_time_to_live_in_seconds" "86400"}}
cas.ticket.tgt.primary.time-to-kill-in-seconds={{ .Config.GetOrDefault "session_tgt/time_to_kill_in_seconds" "36000"}}
########################################################################################################################

########################################################################################################################
# OIDC - Provider Mode
# Configuration guide:
# Properties: https://apereo.github.io/cas/6.1.x/configuration/Configuration-Properties-Common.html#delegated-authentication-openid-connect-settings
# ----------------------------------------------------------------------------------------------------------------------
{{ if ne (.Config.Get "oidc/enabled") "false"}}
### path to the discovery url of the provider
cas.authn.pac4j.oidc[0].generic.discovery-uri={{ .Config.GetOrDefault "oidc/discovery_uri" ""}}

### required configuration
cas.authn.pac4j.oidc[0].generic.useNonce=true
cas.authn.pac4j.oidc[0].generic.enabled={{ .Config.Get "oidc/enabled"}}

### name and secret for the client to identify itself by the provider
cas.authn.pac4j.oidc[0].generic.id={{ .Config.Get "oidc/client_id"}}
cas.authn.pac4j.oidc[0].generic.secret={{ .Config.GetAndDecrypt "oidc/client_secret"}}

### Max clock skew
cas.authn.pac4j.oidc[0].generic.max-clock-skew=5

### the client name used to identify the client in the cas application
cas.authn.pac4j.oidc[0].generic.client-name={{ .Config.Get "oidc/display_name"}}

### perform automatic redirects to the configured provider when a user logs into the cas
cas.authn.pac4j.oidc[0].generic.auto-redirect={{if eq (.Config.Get "oidc/optional") "true"}}false{{else}}true{{end}}

### redirect back to the ces after successful logout
cas.authn.pac4j.oidc[0].generic.target-url={{ .Config.GetOrDefault "oidc/redirect_uri" "" }}

### information that are supposed to be contained in the responses of the OIDC provider
cas.authn.pac4j.oidc[0].generic.scope={{ .Config.Get "oidc/scopes"}}
cas.authn.pac4j.oidc[0].generic.responseType=code

### preferred algorithm to use for the open id connect jwt tokens
cas.authn.pac4j.oidc[0].generic.preferredJwsAlgorithm=RS256

### the attribute that should be used as the principal id
ces.services.oidcPrincipalsAttribute={{ .Config.GetOrDefault "oidc/principal_attribute" ""}}

### attribute mapping
ces.services.attributeMapping={{ .Config.Get "oidc/attribute_mapping"}}
{{ end }}
########################################################################################################################

########################################################################################################################
# OIDC+OAuth2 - Client Mode
# Configuration guide:
# OIDC Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#openid-connect
# OAuth2 Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#oauth2
# ----------------------------------------------------------------------------------------------------------------------
# OIDC
# assigns the issuer of oidc correctly
cas.authn.oidc.core.issuer=${cas.server.prefix}/oidc
# ----------------------------------------------------------------------------------------------------------------------
# OAuth2
# Time for the code to expire
cas.authn.oauth.code.timeToKillInSeconds=30
cas.authn.oauth.code.numberOfUses=1
# Access Token (Session) is valid for 1 day (= 86000 seconds)
cas.authn.oauth.accessToken.timeToKillInSeconds=86000
cas.authn.oauth.accessToken.maxTimeToLiveInSeconds=86000
########################################################################################################################
