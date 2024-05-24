# Setup for the integration tests

This section describes the steps required to properly run the integration tests.

## Requirements

* It is necessary to install the program `yarn`

## Configuration

In order for all integration tests to work properly, some data must be configured beforehand.

**integrationTests/cypress.json** [[Link to file](../../integrationTests/cypress.json)]

1) The base URL must be adapted to the host system.
   For this the field `baseUrl` has to be adjusted to the host FQDN (`https://local.cloudogu.com`).
2) Other aspects must be configured.
   These are set as environment variables in the `cypress.json`:
- `DoguName` - Determines the name of the current dogu and will be used in routing.
- `MaxLoginRetries` - Determines the number of login attempts before a test fails.
- `AdminUsername` - The username of the CES admin.
- `AdminPassword` - The password of the CES admin.
- `AdminGroup` - the user group for CES administrators.
- `ClientID` - The client ID of the integration test client (default:`inttest`).
- `ClientSecret` - The client secret in plain text form (default:`integrationTestClientSecret`).

A sample `cypress.json` looks like this:
```json
{
   "baseUrl": "https://192.168.56.2",
   "env": {
      "doguName": "redmine",
      "MaxLoginRetries": 3,
      "AdminUsername": "ces-admin",
      "AdminPassword": "ecosystem2016",
      "AdminGroup": "cesAdministrators",
      "ClientID" : "inttest",
      "ClientSecret" : "integrationTestClientSecret",
      "PasswordHintText": "Contact your admin",
      "PrivacyPolicyURL": "https://www.triology.de/",
      "TermsOfServiceURL": "https://www.itzbund.de/",
      "ImprintURL": "https://cloudogu.com/"
   }
}
```

## Preparing the integration tests

In order for the integration tests for CAS to run successfully, the following steps must be performed beforehand:

**Step 1:**

A registered service must be created for CAS to allow the tests to communicate with CAS endpoints. This can be easily simulated by writing the following keys to etcd: 
```bash
   etcdctl mkdir /dogu/inttest
   etcdctl set /dogu/inttest/0.0.1 '{"Name": "official/inttest", "Dependencies":["cas"]}'
   etcdctl set /dogu/inttest/current "0.0.1"
```
Now there is an "empty" Dogu for which the CAS registers a service. This is used by the integration test to communicate with the necessary endpoints. The name of the empty dogu must match the value for the `ClientID` from the `cypress.json`.

**Step 2**

In order for our OAuth tests to be successful, we need to create a service account in CAS. Dis we can also simulate by storing a service account in etcd under the CAS path:
```bash
etcdctl set /config/cas/service_accounts/inttest "9e4a414957a0c1f5446b522fb7703e7b761ce904986de7904bf5504f92d143d9"
```
Here `inttest` must correspond to the name of the "empty" dogus from the first step. The value is the configured client secret from the `cypress.json` as SHA-256 hash.

**Step 3**

In order for our tests for the password forgetting functionality to be carried out, we must define a text in the CAS that is to be displayed when the password forgetting button is clicked.
A corresponding entry in etcd can be configured in the following way:

```bash
etcdctl set /config/cas/forgot_password_text 'Contact your admin'
```

The value expected by the tests is defined in `cypress.json` under the attribute `PasswordHintText`.

**Step 4**

In order to run our tests for the legal URLs like the imprint, corresponding URLs must be defined in the CAS to be displayed in the footer.
Corresponding entries in etcd can be configured in the following way:

```bash
etcdctl set /config/cas/legal_urls/imprint 'https://cloudogu.com/'
etcdctl set /config/cas/legal_urls/privacy_policy 'https://www.triology.de/'
etcdctl set /config/cas/legal_urls/terms_of_service 'https://docs.cloudogu.com/'
```

The URLs expected by the tests are defined in `cypress.json` under the attributes `PrivacyPolicyURL`, `TermsOfServiceURL` and `ImprintURL`.

### Preparation: OIDC Provider

**Step 1:** Start Keycloak on host machine and import realm (**Attention:The path to the JSON must be adjusted!**)

```bash
docker run --name kc -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 9000:8080 -e KEYCLOAK_IMPORT="/realm-cloudogu.json -Dkeycloak.profile. feature. upload_scripts=enabled" -v /vagrant/containers/cas/integrationTests/keycloak-realm/realm-cloudogu.json:/realm-cloudogu.json quay.io/keycloak/keycloak:15.0.2
```

**Step 2:** Configuration for the CAS in the CES:

```bash
etcdctl set /config/cas/oidc/enabled "true"
etcdctl set /config/cas/oidc/discovery_uri "http://192.168.56.1:9000/auth/realms/Cloudogu/.well-known/openid-configuration"
etcdctl set /config/cas/oidc/client_id "casClient"
etcdctl set /config/cas/oidc/display_name "MyProvider"
etcdctl set /config/cas/oidc/optional "true"
etcdctl set /config/cas/oidc/scopes "openid email profile groups"
etcdctl set /config/cas/oidc/attribute_mapping "email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName"
```

**Step 3:** Via `cesapp edit-config cas` set the oidc/client_secret to `c21a7690-1ca3-4cf9-bef3-22f37faf5144`. This will then be stored correctly encrypted.

## Starting the integration tests

The integration tests can be started in two ways:

1. with `yarn cypress run` the tests start only in the console without visual feedback.
   This mode is useful when execution is the main focus.
   For example, in a Jenkins pipeline.
   
1. `yarn cypress open` starts an interactive window where you can run, visually observe and debug the tests.
   This mode is especially useful when developing new tests and finding bugs.
