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

#========================================
# static user (TODO: needs to be removed for release)
#========================================
cas.authn.accept.users=cesadmin::cesadmin
cas.authn.attributeRepository.stub.attributes.mail=admin@properties.local
cas.authn.attributeRepository.stub.attributes.cn=Admin
cas.authn.attributeRepository.stub.attributes.givenName=Adam
cas.authn.attributeRepository.stub.attributes.surname=Strator
cas.authn.attributeRepository.stub.attributes.displayName=adminDN
cas.authn.attributeRepository.stub.attributes.groups=cesadmin

ces.services.stage={{ .GlobalConfig.GetOrDefault "stage" "production" }}
ces.services.allowedAttributes=username,cn,mail,groups,givenName,surname,displayName