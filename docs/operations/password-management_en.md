# Password Management in CAS

Password management is enabled in CAS. This allows users with an expired password to set
a new password directly within CAS. Users can also have a link for resetting their password sent
to them by email if they have forgotten their password.

These features are only active when an `embedded` LDAP (that is, the LDAP Dogu) is used.

## Features of Password Management

### Password Change for Expired Passwords

If a user logs in with an expired password, they are redirected to a page where
they can change their password. The password is changed directly in the configured LDAP. This is possible because
the service account used for LDAP has write permissions in LDAP.

After changing the password, the user is redirected to a confirmation page and must then log in again
with the newly changed password.

### Reset Password via a Link Sent by Email

Using the `Reset password` function, a user can have a link for resetting their password
sent to them by email. After entering their username, the link is sent to the email address stored in LDAP
for that user. Using this link, the user is taken to a page in CAS where they can set a new password.

## Configuration of Password Management in CAS

Password management is enabled via specific CAS properties. The following two properties
are required for the general activation of password management.

* `cas.authn.pm.core.enabled=true` - with the value `true`, indicates that password management is enabled.
* `cas.authn.pm.core.password-policy-pattern` - the password policy is defined here in the form of a regular expression.
  The regular expression is assembled by CAS at startup. The individual rules, which characters
  must be included and what length the password must have, can be configured via etcd entries. For details,
  see the section [Configuration of Password Rules in etcd](#configuration-of-password-rules).

For the user to be able to change their password directly in LDAP via CAS, the corresponding LDAP properties
for password management must be set. These properties can reference the values of the general LDAP properties.

* cas.authn.pm.ldap[0].type - the LDAP variant. For the LDAP Dogu this is `GENERIC`.
* cas.authn.pm.ldap[0].ldap-url - the URL of the LDAP.
* cas.authn.pm.ldap[0].base-dn - the base DN (Distinguish Name) of the LDAP to be used. The base DN specifies the LDAP
  entry under which the users to be authenticated can be found.
  Example: `ou=People,o=ces.local,dc=cloudogu,dc=com`. Here, the entries assigned to the organizational unit
  (OU) `people` are taken into account.
* cas.authn.pm.ldap[0].search-filter - the filter for searching users.
  Example: `(&(objectClass=person)(uid={user}))`. This search filter looks for entries with the object class Person
  using the user ID.
* cas.authn.pm.ldap[0].bind-dn - the bind DN of the user that is to be used for the connection with LDAP. This
  user performs the changes in LDAP.
* cas.authn.pm.ldap[0].bind-credential - the login information (the password) that is to be used for the connection to LDAP.

In addition to the general email setup, the following properties must be set
for sending the password reset link:

* cas.authn.pm.reset.mail.attribute-name - specifies the name of the mail attribute in LDAP. This value is read from the
  etcd entry `ldap/attribute_mail`.
* cas.authn.pm.reset.mail.from - specifies the email address displayed as the sender of the email. This value
  can be configured via the etcd entry `mail_sender`. If no value is specified, a default value
  is used.
* cas.authn.pm.reset.mail.subject - specifies the subject of the emails. This value can be configured via the
  etcd entry `password_management/reset_password_subject`. If no value is specified, a
  default value is used.
* cas.authn.pm.reset.mail.text - specifies the text of the email. This value can be configured via the
  etcd entry `password_management/reset_password_text`. It is mandatory that the
  text contains a `${url}` placeholder for the password reset link. Umlauts must be specified in encoded form.
  If no value is specified in etcd, a default value is used.
* cas.authn.pm.reset.expiration - defines the validity period of the password reset link. The value
  is specified using the `java.time.Duration` syntax.
* cas.authn.pm.reset.security-questions-enabled - with `false`, indicates that no
  security questions must be answered in order to reset the password.

### Disabling the Password Reset Function

It is possible to disable the password reset function by setting a corresponding etcd entry.
To disable the password reset function, the
value `password_management/enable_password_reset_via_email` must be set to `false`.

Instead of the `Reset password` link, the `Forgot password` button is shown instead, provided
that a value for the `forgot_password_text` entry is stored in the configuration. When a user clicks the `Forgot password` button,
the text stored under `forgot_password_text` is displayed.

## Configuration of Password Rules

Certain rules for passwords can be activated in the ``global-config`` config map. Specifically, it can be configured whether a password
must contain certain characters and what minimum length a password must have.

The respective rule can be activated for the following entries with the value `true`.

* `password-policy/must_contain_capital_letter` - specifies whether the password must contain at least one
  uppercase letter
* `password-policy/must_contain_lower_case_letter` - specifies whether the password must contain at least one
  lowercase letter
* `password-policy/must_contain_digit` - specifies whether the password must contain at least one digit
* `password-policy/must_contain_special_character` - specifies whether the password must contain at least one
  special character

For uppercase letters, the umlauts `Ä`, `Ö`, and `Ü` are included; for lowercase letters, the umlauts `ä`, `ö`, and `u`
as well as `ß` are included. All characters that are neither uppercase letters, lowercase letters, nor digits count as special characters.

The minimum length of the password can be configured via the `password-policy/min_length` entry.
A numeric integer value must be entered here. If no value is specified or a non-integer value is set, the
minimum length is 1.

The values are applied after a restart of CAS.

These values can be configured via `kubectl edit -n ecosystem Configmap global-config`.
