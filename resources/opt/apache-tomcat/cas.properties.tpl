cas.server.name=https://%FQDN%
cas.server.prefix=${cas.server.name}/cas

cas.service-registry.watcher-enabled=true

# Auto-initialize the registry from default JSON service definitions
cas.service-registry.json.location=file:/opt/apache-tomcat/services

cas.authn.accept.users=cesadmin::cesadmin
cas.authn.attributeRepository.stub.attributes.mail=admin@properties.local
cas.authn.attributeRepository.stub.attributes.cn=Admin
cas.authn.attributeRepository.stub.attributes.givenName=Adam
cas.authn.attributeRepository.stub.attributes.surname=Strator
cas.authn.attributeRepository.stub.attributes.displayName=adminDN
cas.authn.attributeRepository.stub.attributes.groups=cesadmin

## for testing
cas.ticket.st.numberOfUses=100
cas.ticket.st.timeToKillInSeconds=10000
cas.ticket.pt.numberOfUses=100
cas.ticket.pt.timeToKillInSeconds=10000