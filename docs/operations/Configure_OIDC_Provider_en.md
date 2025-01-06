# Configuration for using an OIDC provider as an authentication source.

The following section describes the configuration entries for setting up an OIDC Provider for authentication in CAS.

## Prerequisites

A functional OIDC provider is available. A new client must be registered in this provider.
Such registration basically consists of a client ID and a client secret.

## Configuration

#### oidc/enabled
* Configuration key path: `<cas_path>/oidc/enabled`
* Content: specifies whether the CAS should use the configured OIDC provider for delegated authentication.
  This entry is set to 'false' by default.
* Data type: True value
* Valid values: `true`, `false`.

#### oidc/discovery_uri
* Configuration key path: `<cas_path>/oidc/discovery_uri`
* Contents: describes the URI containing the description for the target provider's OIDC protocol. Must point to the openid-connect configuration. This is usually structured as follows: `https://[base-server-url]/.well-known/openid-configuration`.
* Data type: String
* Valid values: `https://[base-server-url]/.well-known/openid-configuration`.

#### oidc/client_id
* Configuration key path: `<cas_path>/oidc/client_id`
* Content: Contains the identifier to be used to identify the CAS to the OIDC provider.
* Data type: String

#### oidc/client_secret
* Configuration key path: `<cas_path>/oidc/client_secret`
* Content: Contains the secret to be used with the client ID to identify the CAS to the OIDC provider.
* Data type: String
* **Encrypted**.

#### oidc/display_name
* Configuration Key Path: `<cas_path>/oidc/display_name`
* Content: The display name is used for the OIDC provider on the user interface.
* Data type: String

#### oidc/redirect_uri
* Configuration Key Path: `<cas_path>/oidc/redirect_uri`
* Content: Specifies where the user will be redirected to after a successful logout.
* Data type: String
* Default value: `<FQDN>/cas/logout`

#### oidc/optional
* Configuration key path: `<cas_path>/oidc/optional`
* Content: specifies whether authentication via the configured OIDC provider is optional. The user will be automatically redirected to the OIDC provider login page if this property is set to 'false'. The 'true' entry makes authentication via the OIDC provider optional. This is done by displaying an additional button for the OIDC provider on the login page of the CAS, which can be used to authenticate with the OIDC provider. By default, this entry is set to 'false'.
* Data type: String

#### oidc/scopes
* Configuration key path: `<cas_path>/oidc/scopes`
* Content: specifies the resource to query against OIDC. Normally, this enumeration should include at least the openid, the user's email, profile information, and the groups assigned to the user. This entry is set to "openid email profile groups" by default.
* Data type: String according to format: `scope1 scope2`.

#### oidc/attribute_mapping
* Configuration key path: `<cas_path>/oidc/attribute_mapping`
* Contents: the attributes provided by OIDC do not exactly match the attributes required by CAS. It is necessary to convert the OIDC attributes to attributes accepted by the CAS. Therefore, this entry should contain rules for converting an attribute provided by the OIDC vendor to an attribute required by the CAS. The rules should be specified in the following format: email:mail,familyname:lastname'. In the given example, the OIDC attributes "email" and "family_name" are converted to "mail" and "surname" respectively. The CAS needs the following attributes to work properly: 'mail,surname,givenName,username,displayName'.
* Data type: String according to format: `fromAttribute:toAttribute,fromAttribute2:toAttribute2`.

#### oidc/principal_attribute
* Configuration key path: `<cas_path>/oidc/principal_attribute`
* Contents: Specifies an attribute that should be used as principal id inside the CES. CAS uses the ID provided by the OIDC provider when this property is empty.
* Data type: Name of OIDC attribute

#### oidc/allowed_groups
* Configuration key path: `<cas_path>/oidc/allowed_groups`
* Inhalt: Specifies a list of OIDC groups that are allowed to log in using delegated authentication. The groups are seperated by comma. An empty list allows access for everyone.
* Datentyp: String according to format: `Group1, Group2`.

#### oidc/initial_admin_usernames
* Configuration key path: `<cas_path>/oidc/initial_admin_usernames`
* Inhalt: Specifies a list of usernames that will be assigned to the CES admin-group, when they first log in.
* Datentyp: String according to format: `User1, User2`.