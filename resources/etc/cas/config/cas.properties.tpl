########################################################################################################################
# General server configuration
# Properties: https://apereo.github.io/cas/6.3.x/configuration/Configuration-Properties.html#cas-server
# ----------------------------------------------------------------------------------------------------------------------
cas.server.name=https://{{ .GlobalConfig.Get "fqdn" }}
cas.server.prefix=${cas.server.name}/cas

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
logging.level.org.apereo.cas.authentication.attribute=DEBUG

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
# Health-Endpoint configuration
# Configuration guide: https://apereo.github.io/cas/7.0.x/monitoring/actuators/Actuator-Endpoint-Health.html#casendppointpropshealth
# ----------------------------------------------------------------------------------------------------------------------
management.endpoint.health.enabled=true
management.endpoint.health.show-details=always
management.endpoints.web.exposure.include=health
cas.monitor.endpoints.endpoint.health.access=ANONYMOUS
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

# OAuth keys
cas.authn.oauth.crypto.encryption.key=8VZlZV8NkZjWxVLmujOYbGqE4p0dk74gYwVQZDRdHQ4QqX6rDNUxl7M2mO9szZDYzYg7hzs8yz60gGxqEMRqlA
cas.authn.oauth.crypto.signing.key=YYgZMkfdC9VdHUJcZfWheM17MHiMyhToM45o4I1sUNBh42lh6x3Chg8VC9YVqBDGpJzXYTtA2VKtaQav78R4XA




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

# Check givenName & surname for empty strings. Older ces-setup versions did write empty strings for these attributes in the cas-config.
{{ $attributeGivenName := .Config.GetOrDefault "ldap/attribute_given_name" "givenName" }}
{{ if eq $attributeGivenName "" }}
  {{ $attributeGivenName = "givenName" }}
{{ end }}
{{ $attributeSurname := .Config.GetOrDefault "ldap/attribute_surname" "sn" }}
{{ if eq $attributeSurname "" }}
  {{ $attributeSurname = "sn" }}
{{ end }}

cas.authn.ldap[0].principal-attribute-id={{ .Config.Get "ldap/attribute_id"}}
cas.authn.ldap[0].principal-attribute-list={{ .Config.Get "ldap/attribute_id"}}:username,cn,{{ .Config.Get "ldap/attribute_mail"}}:mail,{{ $attributeGivenName}}:givenName,{{ $attributeSurname}}:surname,displayName,{{ .Config.Get "ldap/attribute_group"}}:groups
cas.authn.attributeRepository.ldap[0].attributes.groups={{ .Config.Get "ldap/attribute_group"}}

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
# spring.autoconfigure.exclude=org.apereo.cas.config.LdapAuthenticationConfiguration,org.apereo.cas.config.LdapPasswordManagementConfiguration

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
cas.authn.pm.reset.mail.text={{ .Config.GetOrDefault "password_management/reset_password_text" "Hello!\\n\\nSomeone has just requested to change the login details of your CES account. If it was you, please follow the link below to reset them.\\n${url}\\n\\nThis link will expire in 15 minutes.\\n\\nIf you do not wish to reset your credentials, simply ignore this message and nothing will change."}}
cas.authn.pm.reset.expiration=PT15M
cas.authn.pm.reset.security-questions-enabled=false
########################################################################################################################
{{ end }}

########################################################################################################################
# Throttling
# Configuration guide: https://apereo.github.io/cas/7.0.x/authentication/Configuring-Authentication-Throttling.html
# Properties: https://apereo.github.io/cas/7.0.x/authentication/Configuring-Authentication-Throttling.html#failure-throttling
# ----------------------------------------------------------------------------------------------------------------------
{{ $failureThreshold := .Config.GetOrDefault "limit/failure_threshold" "500" }}
{{ if ne $failureThreshold "0" }}
# Authentication Failure Throttling
# Username parameter to use in order to extract the username from the request.
cas.authn.throttle.core.username-parameter=username
cas.authn.throttle.core.app-code=CAS
cas.authn.throttle.failure.code=AUTHENTICATION_FAILED
# The failure threshold rate is calculated as: threshold / range-seconds
cas.authn.throttle.failure.threshold={{ $failureThreshold }}
cas.authn.throttle.failure.range-seconds={{ .Config.GetOrDefault "limit/range_seconds" "10"}}
# The throttled account will be locked out for this many seconds
cas.authn.throttle.failure.throttle-window-seconds={{ .Config.GetOrDefault "limit/lock_time" "600"}}

# Configure clean-up of stale throttle attempts from the in-memory map
cas.authn.throttle.schedule.enabled=true
# wait x seconds to start with cleaning old throttle attempts
cas.authn.throttle.schedule.start-delay=PT10S
# define an interval of x seconds after which stale throttle attempts are removed from the in-memory map
cas.authn.throttle.schedule.repeat-interval=PT{{ .Config.GetOrDefault "limit/stale_removal_interval" "60"}}S
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
# Ticket Expiration Policies
# Configuration guide: https://apereo.github.io/cas/6.6.x/ticketing/Configuring-Ticket-Expiration-Policy.html#ticket-granting-ticket-policies
# Properties: https://apereo.github.io/cas/6.6.x/ticketing/Configuring-Ticket-Expiration-Policy.html#default
# ----------------------------------------------------------------------------------------------------------------------
# only-track-most-recent-session means only most recent STs and PTs are stored so there is only one valid ST / PT
# per service
cas.ticket.tgt.core.only-track-most-recent-session=false
########################################################################################################################

########################################################################################################################
# OIDC - Provider Mode
# Configuration guide:
# Properties: https://apereo.github.io/cas/7.0.x/integration/Delegate-Authentication-Generic-OpenID-Connect.html
# ----------------------------------------------------------------------------------------------------------------------
{{ if eq (.Config.Get "oidc/enabled") "true"}}
cas.authn.pac4j.oidc[0].generic.enabled=true

### path to the discovery url of the provider
cas.authn.pac4j.oidc[0].generic.discovery-uri={{ .Config.GetOrDefault "oidc/discovery_uri" ""}}

### name and secret for the client to identify itself by the provider
cas.authn.pac4j.oidc[0].generic.id={{ .Config.Get "oidc/client_id"}}
cas.authn.pac4j.oidc[0].generic.secret={{ .Config.GetAndDecrypt "oidc/client_secret"}}

### required configuration
cas.authn.pac4j.oidc[0].generic.client-authentication-method=client_secret_basic
cas.authn.pac4j.oidc[0].generic.use-nonce=true

### Max clock skew
cas.authn.pac4j.oidc[0].generic.max-clock-skew=5

### the client name used to identify the client in the cas application
cas.authn.pac4j.oidc[0].generic.client-name={{ .Config.Get "oidc/display_name"}}

### perform automatic redirects to the configured provider when a user logs into the cas
cas.authn.pac4j.oidc[0].generic.auto-redirect-type={{if eq (.Config.Get "oidc/optional") "true"}}NONE{{else}}CLIENT{{end}}

### information that are supposed to be contained in the responses of the OIDC provider
cas.authn.pac4j.oidc[0].generic.scope={{ .Config.Get "oidc/scopes"}}
cas.authn.pac4j.oidc[0].generic.response-type=code

### preferred algorithm to use for the open id connect jwt tokens
cas.authn.pac4j.oidc[0].generic.preferred-jws-algorithm=RS256

### the attribute that should be used as the principal id
cas.authn.pac4j.oidc[0].generic.principal-id-attribute={{ .Config.GetOrDefault "oidc/principal_attribute" ""}}

### redirect back to the ces after successful logout
ces.delegation.oidc.redirect-uri={{ .Config.GetOrDefault "oidc/redirect_uri" "" }}

### attribute mapping
ces.delegation.oidc.attributeMapping={{ .Config.Get "oidc/attribute_mapping"}}

### allowed groups - group-names that are allowed to login
ces.delegation.oidc.allowedGroups={{ .Config.GetOrDefault "oidc/allowed_groups" ""}}

### admin usernames - usernames that will be assigned the admin-group
ces.delegation.oidc.initialAdminUsernames={{ .Config.GetOrDefault "oidc/initial_admin_usernames" ""}}
ces.delegation.oidc.adminGroups={{ .GlobalConfig.GetOrDefault "admin_group" "cesAdmin"}},{{ .GlobalConfig.GetOrDefault "manager_group" "cesManager"}}

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

########################################################################################################################
# JSON Registry
# Configuration guide: https://apereo.github.io/cas/7.0.x/services/JSON-Service-Management.html
# ----------------------------------------------------------------------------------------------------------------------
cas.service-registry.json.location={{if eq (.GlobalConfig.GetOrDefault "stage" "production") "production"}}file:/etc/cas/services/production{{else}}file:/etc/cas/services/development{{end}}
cas.service-registry.json.watcher-enabled=true
cas.service-registry.templates.directory.location=file:/etc/cas/services/templates
# Increase start-delay of scheduler to prevent startup-errors on slow starts
cas.service-registry.schedule.start-delay=PT1M
########################################################################################################################
