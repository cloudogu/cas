# Einen externen LDAP anbinden am Beispiel des docker-sample-ldap
Diese Dokumentation beschreibt, wie ein externer LDAP zum lokalen Testen im EcoSystem eingerichtet werden kann.

Im CES-Multinode kann für lokale Tests ein einfacher OpenLDAP-Pod im Namespace `ecosystem` gestartet werden.
Die hier gezeigte Variante ist bewusst als Testaufbau gedacht: Die LDAP-Daten sind nicht persistent und das Passwort wird der Einfachheit halber direkt in die CAS-Konfiguration geschrieben.

## OpenLDAP-Pod installieren
Die folgenden Kubernetes-Ressourcen erzeugen einen `Deployment`- und einen `Service`-Eintrag für einen einfachen OpenLDAP-Server:

```bash
cat <<'EOF' | kubectl apply -n ecosystem -f -
apiVersion: v1
kind: Service
metadata:
  name: openldap
spec:
  selector:
    app: openldap
  ports:
    - name: ldap
      port: 389
      targetPort: 389
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openldap
spec:
  replicas: 1
  selector:
    matchLabels:
      app: openldap
  template:
    metadata:
      labels:
        app: openldap
    spec:
      containers:
        - name: openldap
          image: osixia/openldap:1.5.0
          imagePullPolicy: IfNotPresent
          args:
            - --copy-service
          env:
            - name: LDAP_ORGANISATION
              value: "Cloudogu"
            - name: LDAP_DOMAIN
              value: "cloudogu.com"
            - name: LDAP_ADMIN_PASSWORD
              value: "admin"
            - name: LDAP_TLS
              value: "false"
          ports:
            - containerPort: 389
              name: ldap
          readinessProbe:
            tcpSocket:
              port: 389
            initialDelaySeconds: 10
            periodSeconds: 5
          livenessProbe:
            tcpSocket:
              port: 389
            initialDelaySeconds: 30
            periodSeconds: 10
EOF
kubectl rollout status -n ecosystem deployment/openldap
```

Der LDAP-Server ist danach im Cluster unter `openldap.ecosystem.svc.cluster.local:389` erreichbar.

## Benutzer und Gruppen im OpenLDAP anlegen

```bash
kubectl exec -n ecosystem -it deployment/openldap -- bash
```

Im Pod kann per `ldapadd` ein einfacher Testbestand angelegt werden:

```bash
cat >/tmp/cas-external-ldap.ldif <<'LDIF_EOF'
dn: o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: organization
o: ces.local

dn: ou=People,o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: organizationalUnit
ou: People

dn: ou=Groups,o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: organizationalUnit
ou: Groups

dn: uid=admin,ou=People,o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
uid: admin
cn: Administrator
sn: Administrator
givenName: Admin
displayName: Administrator
mail: admin@cloudogu.local
userPassword: adminpw

dn: cn=cesAdmin,ou=Groups,o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: groupOfNames
cn: cesAdmin
member: uid=admin,ou=People,o=ces.local,dc=cloudogu,dc=com

dn: cn=cesManager,ou=Groups,o=ces.local,dc=cloudogu,dc=com
objectClass: top
objectClass: groupOfNames
cn: cesManager
member: uid=admin,ou=People,o=ces.local,dc=cloudogu,dc=com
LDIF_EOF

ldapadd -x -D "cn=admin,dc=cloudogu,dc=com" -w admin -f /tmp/cas-external-ldap.ldif
exit
```

Optional kann die Anlage mit einer LDAP-Suche geprüft werden:

```bash
kubectl exec -n ecosystem -it deployment/openldap -- ldapsearch -x -D "cn=admin,dc=cloudogu,dc=com" -w admin -b "o=ces.local,dc=cloudogu,dc=com" "(uid=admin)"
```

## CAS im Multinode konfigurieren
Die CAS-Konfiguration wird im Multinode-System in der ConfigMap `cas-config` gepflegt.

```bash
kubectl edit -n ecosystem configmap cas-config
```

Dort in `data.config.yaml` den LDAP-Block ergänzen oder anpassen:

```yaml
data:
  config.yaml: |
    ldap:
      ds_type: "external"
      host: "openldap"
      port: "389"
      base_dn: "o=ces.local,dc=cloudogu,dc=com"
      connection_dn: "cn=admin,dc=cloudogu,dc=com"
      password: "admin"
      search_filter: "(objectClass=person)"
      encryption: "none"
      attribute_id: "uid"
      attribute_given_name: "givenName"
      attribute_surname: "sn"
      attribute_fullname: "cn"
      attribute_mail: "mail"
      attribute_group: "memberOf"
      group_attribute_name: "cn"
      group_base_dn: "ou=Groups,o=ces.local,dc=cloudogu,dc=com"
      group_search_filter: "(member={0})"
```

## Einloggen
Nach dem Neustart des CAS kann die Anmeldung mit dem angelegten Benutzer erfolgen:

* Benutzername: `admin`
* Passwort: `adminpw`

## Hinweise
* Diese Anleitung ist für lokale Tests gedacht. Für einen produktiven Betrieb sollte das LDAP-Passwort nicht im Klartext in der ConfigMap stehen, sondern im Secret `cas-config` abgelegt werden.
