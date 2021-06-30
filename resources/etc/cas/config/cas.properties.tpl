#========================================
# General server configuration
#========================================
cas.server.name=https://{{ .GlobalConfig.Get "fqdn" }}
cas.server.prefix=${cas.server.name}/cas
logging.config=file:/etc/cas/config/log4j2.xml

#========================================
# Security configuration
#========================================
cas.securityContext.serviceProperties.adminRoles=ROLE_ADMIN
cas.securityContext.casProcessingFilterEntryPoint.loginUrl=${server.prefix}/login
cas.securityContext.ticketValidator.casServerUrlPrefix=${server.prefix}
# IP address or CIDR subnet allowed to access the /status URI of CAS that exposes health check information
cas.securityContext.status.allowedSubnet=127.0.0.1

#========================================
# Unique CAS node name
# host.name is used to generate unique Service Ticket IDs and SAMLArtifacts.  This is usually set to the specific
# hostname of the machine running the CAS node, but it could be any label so long as it is unique in the cluster.
#========================================
host.name=cas.{{ .GlobalConfig.Get "domain" }}

# Disable static users
cas.authn.accept.enabled=false

#========================================
# LDAP
#========================================

#========================================
# General properties
#========================================
cas.authn.ldap[0].ldap-url={{ .Env.Get "LDAP_PROTOCOL" }}://{{ .Config.Get "ldap/host"}}:{{ .Config.Get "ldap/port"}}
cas.authn.ldap[0].type=AUTHENTICATED

# LDAP connection timeout in milliseconds
cas.authn.ldap[0].connect-timeout=3000

# Whether to use StartTLS (probably needed if not SSL connection)
ldap.useStartTLS={{ .Env.Get "LDAP_STARTTLS" }}

#========================================
# LDAP connection pool configuration
#========================================
cas.authn.ldap[0].min-pool-size=3
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

# Base DN of users to be authenticated
cas.authn.ldap[0].base-dn={{ .Env.Get "LDAP_BASE_DN" }}

# Search filter used for configurations that require searching for DNs
cas.authn.ldap[0].search-filter={{ .Env.Get "LDAP_SEARCH_FILTER" }}

# Search filter used for configurations that require searching for DNs
cas.authn.ldap[0].dn-format=uid=%s,ou=Accounts,{{ .Env.Get "LDAP_BASE_DN" }}

cas.authn.ldap[0].principal-attribute-list=uid:username,cn,mail,givenName,sn:surname,displayName,memberOf:groups

ces.services.stage={{ .GlobalConfig.GetOrDefault "stage" "production" }}
ces.services.allowedAttributes=username,cn,mail,givenName,surname,displayName,groups

#========================================
# OAuth
#========================================
# Time for the code to expire
cas.authn.oauth.code.timeToKillInSeconds=30
cas.authn.oauth.code.numberOfUses=1

# Access Token (Session) is valid for 1 day = 86000 seconds
cas.authn.oauth.accessToken.timeToKillInSeconds=86000
cas.authn.oauth.accessToken.maxTimeToLiveInSeconds=86000
