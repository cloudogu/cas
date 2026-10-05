# Connecting an External LDAP Using docker-sample-ldap as an Example
This documentation describes how an external LDAP can be set up in the ecosystem for local testing.

In CES Multinode, a simple OpenLDAP pod can be started in the `ecosystem` namespace for local tests.
The variant shown here is intentionally intended as a test setup: the LDAP data is not persistent and, for simplicity, the password is written directly into the CAS configuration.

## Install the OpenLDAP Pod
The following Kubernetes resources create a `Deployment` and a `Service` entry for a simple OpenLDAP server:

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

The LDAP server can then be reached within the cluster at `openldap.ecosystem.svc.cluster.local:389`.

## Create Users and Groups in OpenLDAP

```bash
kubectl exec -n ecosystem -it deployment/openldap -- bash
```

Inside the pod, a simple test dataset can be created with `ldapadd`:

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

Optionally, the setup can be verified with an LDAP search:

```bash
kubectl exec -n ecosystem -it deployment/openldap -- ldapsearch -x -D "cn=admin,dc=cloudogu,dc=com" -w admin -b "o=ces.local,dc=cloudogu,dc=com" "(uid=admin)"
```

## Configure CAS in Multinode
The CAS configuration is maintained in the `cas-config` ConfigMap in the Multinode system.

```bash
kubectl edit -n ecosystem configmap cas-config
```

Add or adjust the LDAP block in `data.config.yaml` there:

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

## Logging In
After restarting CAS, login can be performed with the created user:

* Username: `admin`
* Password: `adminpw`

## Notes
* This guide is intended for local tests. For production use, the LDAP password should not be stored in plain text in the ConfigMap, but in the `cas-config` secret.
