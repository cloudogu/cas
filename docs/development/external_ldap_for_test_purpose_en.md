# Connecting an external LDAP using the docker-sample-ldap as an example.
This documentation describes how to set up an external ldap for local testing in EcoSystem.

## Set up docker-sample-ldap.
* Clone repository: `git clone https://github.com/cloudogu/docker-sample-ldap`
* `cd docker-sample-ldap && make build`
* `docker run -p 389:389 -d --restart=always cloudogu/sample-ldap`

## Cas Configure
* Run the following script in EcoSystem
```bash
etcdctl set config/cas/ldap/host 192.168.56.1
etcdctl set config/cas/ldap/port 389
etcdctl set config/cas/base_dn dc=cloudogu,dc=com
etcdctl set config/cas/connection_dn cn=usermgt_x53eMC,ou=Special Users,o=ces.local,dc=cloudogu,dc=com
docker exec -it cas bash -c "doguctl config -e ldap/password eO2H6WzCOgrpZzvL"
etcdctl set config/cas/ldap/search_filter "(objectClass=person)"
etcdctl set config/cas/ldap/ds_type external
etcdctl set config/cas/ldap/encryption none
etcdctl set config/cas/ldap/attribute_id uid
etcdctl set config/cas/ldap/attribute_given_name givenName
etcdctl set config/cas/ldap/attribute_surname sn
etcdctl set config/cas/ldap/attribute_fullname cn
etcdctl set config/cas/ldap/attribute_mail mail
etcdctl set config/cas/ldap/attribute_group memberOf
etcdctl set config/cas/ldap/group_attribute_name member
etcdctl set config/cas/ldap/group_base_dn o=ces.local,dc=cloudogu,dc=com
etcdctl set config/cas/ldap/group_search_filter "(objectClass=groupOfNames)"
docker restart cas
```

## Log in
Cas is now connected to the external LDAP and you can log in to CES with `admin:adminpw`.