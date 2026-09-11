# PAT workflow for developers

Personal Access Tokens (PATs) provide alternative credentials for CAS authentication, for example for automated API access. CAS manages the tokens, validates them during authentication, and restricts service ticket issuance according to their scopes. Usermgt provides the interface for creating, listing, and deleting tokens.

This guide describes the current implementation, including `PATAuthenticationHandler` and `PATServiceTicketFactory`.

## Prerequisites and flags

You need a CAS build with PAT support, a working LDAP connection to resolve the token owner, and writable, persistent storage for the PAT database. Management through the user interface also requires a Usermgt build with PAT support. The target Dogu must forward authentication to CAS and be allowed as a CAS service; a PAT does not automatically enable Basic Auth in arbitrary Dogus.

| Dogu / key | PAT without 2FA | PAT with 2FA | Purpose |
| --- | --- | --- | --- |
| CAS: `pat/enabled` | `true` | `true` | Enables management, the PAT authentication handler, and scope validation. Default: `false`. |
| CAS: `experimental/totp/activate` | `false` | `true` | Enables the global TOTP provider `mfa-gauth`. Default: `false`. |
| CAS: `experimental/totp/api_user_name` | Required | Required | Technical API username stored in encrypted form. |
| CAS: `experimental/totp/api_user_password` | Required | Required | Technical API password stored in encrypted form. |
| CAS: `pat/database_url` | Optional | Optional | Default: `jdbc:sqlite:/var/ces/config/pats.db`. SQLite is the currently implemented provider. |
| Usermgt: `experimental/totp/api_user_name` | Required for Usermgt | Required for Usermgt | Must match the value in CAS. Store in encrypted form. |
| Usermgt: `experimental/totp/api_user_password` | Required for Usermgt | Required for Usermgt | Must match the value in CAS. Store in encrypted form. |

When using the Dogu template, the API credentials are **required even without 2FA**, although their keys are located under `experimental/totp/`. The CAS template uses them to set `spring.security.user.name` and `spring.security.user.password`, independently of whether TOTP is enabled. If explicit credentials are missing while the PAT service is enabled, CAS fails to start. An automatically generated Spring Security password is not sufficient.

Usermgt does not currently require its own `pat/enabled` flag. Its startup script passes the credentials to the JVM system properties `cas.mfa.user` and `cas.mfa.password`, independently of whether TOTP is enabled. `PATResource` uses these properties for backend requests to CAS.

### Example: Configuring the Dogus

The following commands apply to a Docker development environment with containers named `cas` and `usermgt`. Replace the username and password with your own values; `-e` stores the API credentials in encrypted form.

```bash
docker exec cas doguctl config pat/enabled true
docker exec cas doguctl config experimental/totp/activate false
docker exec cas doguctl config -e experimental/totp/api_user_name '<API-USER>'
docker exec cas doguctl config -e experimental/totp/api_user_password '<API-PASSWORD>'

# Only required for management through Usermgt:
docker exec usermgt doguctl config -e experimental/totp/api_user_name '<API-USER>'
docker exec usermgt doguctl config -e experimental/totp/api_user_password '<API-PASSWORD>'

cesapp restart cas
cesapp restart usermgt
```

For operation with 2FA, set `experimental/totp/activate` in CAS to `true` instead and restart CAS. At startup, CAS generates the TOTP keys under `totp/encryption_key`, `totp/signing_key`, and `totp/scratch_codes/encryption_key` if TOTP is enabled and `totp/encryption_key` has not yet been set. If other keys are missing while `totp/encryption_key` already exists, the startup script does not automatically supply them. TOTP registrations are stored in `/etc/cas/gauth/gauths.json`. Interactive login requires a configured authenticator app; see [Two-factor authentication](../operations/two_factor_authentication_en.md).

In the CAS Helm chart, the corresponding PAT values are `configuration.normal.pat.enabled` and `configuration.normal.pat.database_url`. Supply credentials through the sensitive configuration of the respective deployment environment. After configuration changes, restart the affected Dogus so that templates or JVM properties are updated.

### Starting the CAS application directly

Without the Dogu template, at least the following Spring properties must be supplied:

```properties
personal-acces-token-service.enabled=true
personal-acces-token-service.database-url=jdbc:sqlite:/tmp/pats.db
spring.security.user.name=<API-USER>
spring.security.user.password=<API-PASSWORD>
```

The spelling `personal-acces-token-service`, with only one `s` in `acces`, is the actual configuration prefix. LDAP and service registry configuration are also required. Flyway automatically creates the PAT schema at startup; manual SQL initialization is not necessary.

To start directly with TOTP, also include the MFA configuration and keys from the [CAS properties template](../../resources/etc/cas/config/cas.properties.tpl). This includes the following bypass properties, which are essential for PATs:

```properties
cas.authn.mfa.gauth.bypass.principal-attribute-name=patAuthentication
cas.authn.mfa.gauth.bypass.principal-attribute-value=true
```

Inside the Dogu, the template sets these automatically when TOTP is enabled.

## Workflow: Creating and managing PATs

1. The user logs in to Usermgt normally, including the second factor if TOTP is enabled, and opens the security page.
2. The interface calls the Usermgt backend at `/usermgt/api/pats`. The backend determines the owner from the authenticated Shiro principal.
3. Usermgt calls `/cas/api/users/{userId}/pats` using the **technical API credentials** via HTTP Basic. The `userId` identifies the intended token owner.
4. CAS validates the input and generates a token prefixed with `pat_` from 32 random bytes. It stores the token's SHA-256 fingerprint and metadata; the plaintext token is returned only in the create response.
5. The interface displays the token once. Subsequent list and detail requests return metadata only. Deletion removes the record from the database.

The CAS management API uses its own stateless Basic Auth filter chain, without a browser session or TOTP prompt. A personal PAT does not replace the technical API credentials here. CAS does not require a PAT-specific role for this API: an accepted technical account can manage PATs for arbitrary user IDs. The calling backend must therefore reliably derive the owner from its session.

| Method and path relative to `/cas` | Result |
| --- | --- |
| `POST /api/users/{userId}/pats` | `201`, metadata and a one-time `token` value |
| `GET /api/users/{userId}/pats` | `200`, list of metadata |
| `GET /api/users/{userId}/pats/{id}` | `200`, individual metadata record |
| `DELETE /api/users/{userId}/pats/{id}` | `204`, token deleted |

Example create request body:

```json
{
  "displayName": "Usermgt automation",
  "scope": "/usermgt",
  "expiresAt": "2027-12-31T23:59:59Z"
}
```

`displayName` is required and limited to 255 characters. `expiresAt` is optional and must be in the future if specified; without an expiration time, the token remains valid indefinitely. `scope` may contain at most 1000 characters. A missing or empty scope becomes `/*`, allowing all service paths. Unknown JSON fields are rejected.

## Workflow: Logging in with a PAT

1. The client submits the owner's username and the complete token as the password to CAS authentication. For a suitable Dogu, this can happen through its Basic Auth integration; when using the CAS REST API directly, `username` and `password` are submitted as form data.
2. `PATAuthenticationHandler` detects the password prefix `pat_`, looks up the fingerprint, and checks the expiration time. The supplied username must exactly match the stored `userId`.
3. CAS resolves the user, including groups, through LDAP and sets the principal attributes `patScope` and `patAuthentication=true`.
4. If TOTP is enabled, the configured bypass applies based on `patAuthentication=true`. PAT login therefore does not require an additional TOTP code. Regular password login remains subject to TOTP.
5. When issuing a service ticket, `PATServiceTicketFactory` checks the requested service's URL path against `patScope`. The ticket is issued only if the scope matches. Normal service admission rules and permissions in the target Dogu still apply.

| Configuration | Regular interactive password login | PAT login |
| --- | --- | --- |
| PAT enabled, TOTP disabled | Without TOTP | Token and scope validation, without TOTP |
| PAT enabled, TOTP enabled | With TOTP | Token and scope validation, TOTP bypass |
| PAT disabled | According to TOTP configuration | PAT handler and PAT management are not enabled |

The implemented PAT login flow uses username/password credentials. This code does not provide a general `Authorization: Bearer <PAT>` endpoint.

### Scope semantics and ticket lifetime

Scopes are comma-separated paths, for example `/redmine,/usermgt`. `/usermgt` allows `/usermgt` and descendant paths such as `/usermgt/api/users`, but not `/usermgt-other`. `/*` allows all nonempty paths. A trailing slash is removed for comparison. Other wildcards such as `/usermgt/*` have no special glob semantics.

The check applies to the path of the **CAS service URL**, not automatically to the path of every subsequent API request. Host and scheme are not compared in this PAT scope check; the CAS service registry remains relevant for service admission. At creation time, the scope is not validated against installed Dogus.

Deletion or expiration prevents subsequent PAT authentication. The PAT code does not actively revoke tickets or Dogu sessions that have already been issued. The service ticket factory checks the scope stored in the TGT without checking the token's validity in the database again. The check applies only if the principal contains `patScope` as a list with a string in the first position. If this attribute is absent or has a different type, the factory delegates to CAS without a PAT scope check. When changing principal resolution, verify that this attribute is preserved through to the TGT. Use a new TGT when repeating authentication tests.

## Testing the workflow manually

The management API can be exercised using [pat-tests.sh](pat-tests.sh). Bash, curl, and jq are required. Be sure to set the environment variables for your own development instance; the script contains example credentials and uses `curl -k`, which disables TLS certificate verification.

```bash
export CAS_URL='https://ces.example.org/cas'
export SA_USER='<API-USER>'
read -r -s -p 'API password: ' SA_PASSWORD
export SA_PASSWORD
export USER_ID='<LDAP-USERNAME>'

bash docs/development/pat-tests.sh create --scope /usermgt --displayname 'Workflow test'
bash docs/development/pat-tests.sh list
bash docs/development/pat-tests.sh get '<PAT-ID>'
bash docs/development/pat-tests.sh invalid-auth
```

Use the `token` from the create response for authentication; the `id` is needed only for management operations. A direct test of the CAS ticket flow looks like this:

```bash
read -r -s -p 'PAT: ' PAT_TOKEN
curl --fail-with-body -i "$CAS_URL/v1/tickets" \
  --data-urlencode "username=$USER_ID" \
  --data-urlencode "password=$PAT_TOKEN"

# Copy the TGT ID from the successful response's Location header:
TGT_ID='<TGT-ID>'
curl --fail-with-body -i "$CAS_URL/v1/tickets/$TGT_ID" \
  --data-urlencode 'service=https://ces.example.org/usermgt'
```

Replace the service URL with an actually registered URL in your own instance. For a negative scope test, request a registered target outside `/usermgt` using the same TGT: ticket issuance must fail. Then delete the token and attempt a new login:

```bash
bash docs/development/pat-tests.sh delete '<PAT-ID>'

# Expected: A new login with the deleted token fails.
curl --fail-with-body -i "$CAS_URL/v1/tickets" \
  --data-urlencode "username=$USER_ID" \
  --data-urlencode "password=$PAT_TOKEN"
```

Also test an incorrect owner, an incorrect token, an expired token, and both TOTP configurations. With TOTP enabled, a fresh regular browser login must require the second factor, while PAT login proceeds without a TOTP code. The management script alone tests neither authentication nor the MFA bypass.

## Practical login example: Accessing the Usermgt API with a PAT

This example reads the current user's account through `GET /usermgt/api/account`. It requires no administrator role and exercises the complete flow from the API client through CAS to the Dogu response. Run the commands from the CAS repository in the same Bash session. The existing script is named [pat-tests.sh](pat-tests.sh).

### 1. Prerequisites

CAS requires `pat/enabled=true`, a reachable LDAP user, and technical API credentials. In the Dogu template, these come from the encrypted keys `experimental/totp/api_user_name` and `experimental/totp/api_user_password`, even when TOTP is disabled. Restart CAS after changes. Usermgt must be reachable and its configured CAS service URL must be allowed in CAS; its path must be covered by the scope `/usermgt`.

Without 2FA, set `experimental/totp/activate=false`. With 2FA, set `experimental/totp/activate=true`; the CAS template then configures the GAuth bypass for the principal attribute `patAuthentication` with the value `true`. PAT login therefore requires no TOTP code. A fresh regular browser login with the user's password still requires TOTP. API credentials in Usermgt itself are needed for managing PATs through the interface, not for this Basic Auth request using an existing PAT.

### 2. Create a token using the management script

```bash
export CAS_URL='https://ces.example.org/cas'
export USERMGT_URL='https://ces.example.org/usermgt'
export USER_ID='<LDAP-USERNAME>'
export SA_USER='<TECHNICAL-API-USER>'
read -r -s -p 'Technical API password: ' SA_PASSWORD
export SA_PASSWORD

bash docs/development/pat-tests.sh create \
  --scope /usermgt --displayname 'Usermgt API Login'
```

Expect `201 Created`. Copy the `id` from the JSON for subsequent deletion and the complete `token`, including `pat_`. `USER_ID` must exactly match the token owner. The script requires Bash, curl, and jq; it uses `curl -k` for development instances, disabling TLS certificate verification. The direct curl requests below verify certificates; for a private CA, add `--cacert /path/to/ca.pem`.

```bash
export PAT_ID='<ID-FROM-CREATE-RESPONSE>'
read -r -s -p 'Complete PAT: ' PAT_TOKEN
```

### 3. Make the actual API request with the PAT

```bash
curl --fail-with-body --silent --show-error --include \
  --basic --user "$USER_ID:$PAT_TOKEN" \
  --header 'Accept: application/json' \
  "$USERMGT_URL/api/account"
```

Here the password field contains the PAT and the username identifies the LDAP user. `SA_USER` and `SA_PASSWORD` are used exclusively for management. An `Authorization: Bearer` header is not supported by this login flow. The request sends no existing session cookies and does not follow login redirects.

Expect `200 OK` with the current user's account as JSON. An HTML login page or redirect is not a successful API login. Internally, the following happens:

1. Usermgt passes the Basic Auth credentials to `CasRestAuthenticationRealm`.
2. `CasRestClient` submits `username` and the PAT as `password` to `POST /cas/v1/tickets`. CAS checks the fingerprint, expiration, and owner, and resolves the user through LDAP. A successful response contains `201` and the TGT URL in the `Location` header.
3. Usermgt uses this TGT to request a service ticket for its configured `CasConfiguration.service` URL. CAS checks the URL path against `patScope` and returns `200` with the service ticket on success.
4. Usermgt validates the ticket through `Saml11TicketValidator`, adopts the verified principal, and returns the current user's account.

The scope applies to the configured CAS service URL, not to each REST endpoint individually. `/usermgt` covers that path and descendant paths, but not `/usermgt-other`. It does not replace user permissions: a request to `/usermgt/api/users`, for example, still requires the administrator role. This is why the example uses `/api/account`.

### 4. Check rejection and deletion

For a negative scope test, create a second PAT with `--scope /redmine` and repeat the same Usermgt request with it. Issuing the Usermgt service ticket must fail. Also test an incorrect username and an expired PAT. Check the actual error status in the Dogu response; the PAT management API's JSON error format does not automatically apply to this authentication flow.

Then delete the original PAT and repeat the same API request without session cookies:

```bash
bash docs/development/pat-tests.sh delete "$PAT_ID"

# Expected: Access to the account no longer succeeds.
curl --fail-with-body --silent --show-error --include \
  --basic --user "$USER_ID:$PAT_TOKEN" \
  --header 'Accept: application/json' \
  "$USERMGT_URL/api/account"

unset PAT_TOKEN SA_PASSWORD
```

Deletion returns `204`. The new login must fail; deletion does not actively revoke previously issued tickets or existing sessions. Test the complete scenario with TOTP both disabled and enabled. In contrast, `pat-tests.sh invalid-auth` only checks incorrect technical credentials at the management API.

## Troubleshooting

| Symptom | Checks |
| --- | --- |
| CAS does not start after enabling PATs | Are explicit Spring Security credentials configured? Is the JDBC URL supported and the database directory writable? |
| Management API returns `401` | Check the technical API credentials; do not use a user password or PAT. |
| Usermgt cannot load PATs | Check that both Dogus use the same API credentials, have been restarted, and can reach the configured CAS server URL. The list proxy reports CAS errors as `502`. |
| PAT login fails | Check the complete `pat_` token, exact owner, expiration, and LDAP resolution. |
| TGT is created but service ticket is not | Check the scope against the service URL path, then check the service registry. |
| PAT login requests TOTP | Check the rendered bypass properties and the server-side principal attribute `patAuthentication`. |
| Changes do not take effect | Restart CAS or Usermgt as appropriate and test with fresh tickets/sessions. |

## Code entry points

- [PATServiceConfiguration](../../app/src/main/java/de/triology/cas/pat/config/PATServiceConfiguration.java): Feature flag, security filter chain, handler, and ticket factory.
- [PATService](../../app/src/main/java/de/triology/cas/pat/service/PATService.java): Creation, expiration checks, deletion, and scope semantics.
- [PATAuthenticationHandler](../../app/src/main/java/de/triology/cas/pat/authentication/PATAuthenticationHandler.java): Token authentication and principal attributes.
- [PATServiceTicketFactory](../../app/src/main/java/de/triology/cas/pat/authentication/PATServiceTicketFactory.java): Scope validation before ticket issuance.
- [cas.properties.tpl](../../resources/etc/cas/config/cas.properties.tpl): Mapping of Dogu configuration and TOTP bypass.
- [PAT tests](../../app/src/test/java/de/triology/cas/pat): Unit and security tests for the components involved.
