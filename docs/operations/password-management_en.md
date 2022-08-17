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
* `cas.authn.pm.core.password-policy-pattern` - the password policy is defined here in the form of a regular expression.
  is defined here. The regular expression is assembled by the CAS when it is started. The individual rules, which
  characters and which length the password must have can be configured via etcd entries. Details
  see section [Configuration of password rules in etcd](#configuration-of-password-rules-in-etcd).

Translated with www.DeepL.com/Translator (free version)

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

## Configuration of password rules in etcd

In etcd, certain rules for passwords can be activated. Specifically, it can be configured whether a password must
contain certain characters and what the minimum length of a password must be.

With the value `true` the respective rule can be activated for the following entries.

* `/config/_global/password-policy/must_contain_capital_letter` - specifies whether the password must contain at least
  one capital letter.
* `/config/_global/password-policy/must_contain_lower_case_letter` - specifies whether the password must contain at
  least one lowercase letter.
* `/config/_global/password-policy/must_contain_digit` - specifies if the password must contain at least one digit
* `/config/_global/password-policy/must_contain_special_character` - indicates whether the password must contain at
  least one

For uppercase letters this includes the umlauts `Ä`, `Ö` and `Ü`, for lowercase letters it includes the umlauts `ä`, `ö`
and `ü` and the `ß`. Special characters are all characters that are neither uppercase letters, lowercase letters nor
numbers.

The minimum length of the password can be configured via the entry `/config/_global/password-policy/min_length`. A
numeric integer value must be entered here. If no value is entered or a non-integer value is set, the minimum length is
1 .

The values are used by the CAS after a restart.

It should be noted that these values cannot be configured via `cesapp edit-config cas`, as they are global values. These
values are valid for the entire CES and are therefore not Dogu-specific.