# Password management in CAS

Password management is activated in the CAS. If a password has expired, users can assign a new password directly within
the CAS. Users can also have a link to reset their password sent to them by e-mail if they have forgotten their
password.

These features are only active if an `embedded` LDAP (i.e. the LDAP dogu) is used.

## Password management features

### Password change in case of expired password

If a user logs in with an expired password, they are redirected to a page where they can change his or her password. The
password is changed directly in the configured LDAP. This is possible because the service account used for the LDAP has
write permissions in the LDAP.

After changing the password, the user is redirected to a confirmation page and must then log in again with the changed
password.

### Reset password via link sent by e-mail

Using the `Reset your password` function, a user can have a link sent to him by e-mail to reset his password. After
entering his user name, the link is sent to the user's e-mail address stored in the LDAP. This link takes the user to a
page in CAS where they can set a new password.

## Configuration of the password management in CAS

Password management is activated via certain CAS properties. For the general activation of the Password Management, both
of the following properties are required.

* `cas.authn.pm.core.enabled=true` - indicates with the value `true` that password management is enabled.
* `cas.authn.pm.core.password-policy-pattern=.*` - specifies with the value `.*` (regular expression) that all passwords
  are valid. This property is mandatory and must be specified even if there are no restrictions on password exist.

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

To send the password reset link, in addition to the general email send setup, the following properties must be set:

* cas.authn.pm.reset.mail.attribute-name - specifies the name of the mail attribute in LDAP. This value is taken from
  the etcd entry `ldap/attribute_mail`.
* cas.authn.pm.reset.mail.from - specifies the email address that is displayed as the sender of the email. This value
  can be configured via the etcd entry `mail_sender`. If no value is specified, a default value is used.
* cas.authn.pm.reset.mail.subject - specifies the subject of the emails. This value can be configured via the etcd
  entry `password_management/reset_password_subject`. If no value is specified, a default value is used.
* cas.authn.pm.reset.mail.text - specifies the text of the email. This value can be configured via the etcd
  entry `password_management/reset_password_text`. It is mandatory that the text contains a `%s` as a placeholder for
  the password reset link. Note that umlauts must be encoded. If no value is specified in the etcd a default value is
  used.
* cas.authn.pm.reset.expiration - defines the duration for the validity of the password reset link. The specification is
  done in the `java.time.Duration` syntax.
* cas.authn.pm.reset.security-questions-enabled - specifies with `false` that no security questions have to be answered
  to reset the password. security questions need to be answered to reset the password

### Deactivating the password reset function

It is possible to deactivate the password reset function by setting a corresponding etcd entry. To disable the password
reset function, the value `password_management/enable_password_reset_via_email` must be set to `false`.

Instead of the link `reset password`, the button `forgotten password` is displayed instead - provided that in the etcd a
value for the entry `forgot_password_text` is stored. If a user clicks on the `forgot password` button, the text stored
under `forgot_password_text` is displayed.