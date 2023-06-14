# Einen externen LDAP anbinden am Beispiel des docker-sample-ldap
Diese Dokumentation beschreibt, wie ein externer ldap zum lokalen testen im EcoSystem eingerichtet werden kann.

## Docker-Sample-Ldap aufsetzen
* Repository klonen: `git clone https://github.com/cloudogu/docker-sample-ldap`
* `cd docker-sample-ldap && make build`
* `docker run -p 389:389 -d --restart=always cloudogu/sample-ldap`

## Cas Konfigurieren
* Folgendes Script im EcoSystem ausführen
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

## Einloggen
Cas ist jetzt an das externe LDAP angebunden und es kann sich im CES mit `admin:adminpw` eingeloggt werden. 
