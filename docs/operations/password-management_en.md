# Password management in CAS

Password management is activated in the CAS, so that if a password expires, users can assign a new password directly
within the CAS. This feature is only active if an `embedded` LDAP (i.e. the LDAP-dogu) is used.

## How password management works in CAS

If a user logs in with an expired password, they are redirected to a page where they can change his or her password. The
password is changed directly in the configured LDAP. This is possible because the service account used for the LDAP has
write permissions in the LDAP.

After changing the password, the user is redirected to a confirmation page and must then log in again with the changed
password.

## Configuration of the password management in CAS

Password management is activated via certain CAS properties. For the general activation of the Password Management, both
of the following properties are required.

* `cas.authn.pm.enabled=true` - indicates with the value `true` that password management is enabled.
* `cas.authn.pm.policy-pattern=.*` - specifies with the value `.*` (regular expression) that all passwords are valid.
  This property is mandatory and must be specified even if there are no restrictions on password exist.

In order for the user to be able to change his password in the LDAP directly via the CAS, the corresponding LDAP
property must be set for password management. These properties can reference the values of the general LDAP property
reference.

* cas.authn.pm.ldap[0].type - the variant of the LDAP. For the LDAP dogu this is `GENERIC`.
* cas.authn.pm.ldap[0].ldap-url - the URL of the LDAP.
* cas.authn.pm.ldap[0].base-dn - the base DN (Distinguish Name) of the LDAP to be used. The base DN specifies the LDAP
  entry under which the users to be authenticated can be found. Example: `ou=People,o=ces.local,dc=cloudogu,dc=com`.
  Here, the entries that are assigned to the organisational unit (OU) `people` are taken into account.
* cas.authn.pm.ldap[0].search-filter - the filter for searching for users.
  Example: `(&(objectClass=person)(uid={user}))`. This search filter searches for entries with the object class Person
  based on the user ID.
* cas.authn.pm.ldap[0].bind-dn - the Bind DN of the user to be used when connecting to LDAP. This user makes the changes
  in LDAP.
* cas.authn.pm.ldap[0].bind-credential - the credentials (password) to be used when connecting to the LDAP and are to be
  used.