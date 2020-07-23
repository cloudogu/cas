cas.server.name=https://%FQDN%
cas.server.prefix=${cas.server.name}/cas

cas.service-registry.watcher-enabled=true

# Auto-initialize the registry from default JSON service definitions
cas.service-registry.json.location=file:/opt/apache-tomcat/services

cas.authn.accept.users=admin::admin123
cas.authn.attributeRepository.stub.attributes.mail=admin@properties.local
cas.authn.attributeRepository.stub.attributes.cn=Admin
cas.authn.attributeRepository.stub.attributes.givenName=Adam
cas.authn.attributeRepository.stub.attributes.surname=Strator
cas.authn.attributeRepository.stub.attributes.groups=adminGr

#cas.authn.attribute-repository.json[0].order=0
#cas.authn.attribute-repository.json[0].location=file://etc/cas/attribute-repository.json
#cas.authn.attribute-repository.json[0].id=2