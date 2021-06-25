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

cas.authn.ldap[0].ldap-url=ldap://ldap:389/
cas.authn.ldap[0].base-dn=ou=People,o=ces.local,dc=cloudogu,dc=com
cas.authn.ldap[0].type=AUTHENTICATED
cas.authn.ldap[0].dn-format=uid=%s,ou=People,o=ces.local,dc=cloudogu,dc=com
cas.authn.ldap[0].search-filter=(&(objectClass=person)(uid={user}))
cas.authn.ldap[0].principal-attribute-list=uid:username,cn,mail,givenName,sn:surname,displayName,memberOf:groups
cas.authn.ldap[0].disable-pooling=true

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
