# Setup for the integration tests

This section describes the steps required to run the integration tests correctly.

## Prerequisites

* It is necessary to install the program `yarn`

## Configuration

To ensure that all integration tests work properly, some data must be configured beforehand.

**integrationTests/cypress.config.js** [[Link to file](../../integrationTests/cypress.config.js)]

1) The base URL must be adapted to the host system.
   To do this, the `baseUrl` field must be adapted to the host FQDN (`https://192.168.56.2`)
2) Further aspects must be configured.
   These are set as environment variables in `cypress.config.js`:
- `DoguName` - Determines the name of the current dogus and is used for routing.
- `MaxLoginRetries` - Determines the number of login attempts before a test fails.
- `AdminUsername` - The username of the CES admin.
- `AdminPassword` - The password of the CES admin.
- `AdminGroup` - The user group for CES administrators.
- `ClientID` - The client ID of the integration test client (default:`inttest`)
- `ClientSecret` - The client secret in plain text form (default:`integrationTestClientSecret`)
- `PasswordHintText` - The expected text displayed when clicking on the Forgot password button
- `PrivacyPolicyURL` - The expected link for the privacy policy
- `TermsOfServiceURL` - The expected link for the Terms of Service
- `ImprintURL` - The expected link for the imprint

An example `cypress.config.js` looks like this:
```javascript
module.exports = defineConfig({
    e2e: {
        "baseUrl": "https://192.168.56.2",
        "env": {
            "DoguName": "cas/login",
            "MaxLoginRetries": 3,
            "AdminUsername": "ces-admin",
            "AdminPassword": "Ecosystem2016!",
            "AdminGroup": "CesAdministrators",
            "ClientID": "inttest",
            "ClientSecret": "integrationTestClientSecret",
            "PasswordHintText": "Contact your admin",
            "PrivacyPolicyURL": "https://www.triology.de/",
            "TermsOfServiceURL": "https://www.itzbund.de/",
            "ImprintURL": "https://cloudogu.com/"
        },

        specPattern: ["cypress/e2e/**/*.feature"],
        videoCompression: false,
        setupNodeEvents,
        nonGlobalStepBaseDir: false,
        chromeWebSecurity: false,
        experimentalRunAllSpecs: true,
    }
})
```

## Preparing the integration tests

In order for the integration tests for CAS to run successfully, the following steps must be carried out beforehand:

**Step 1:**

A registered service for CAS must be created so that the tests can communicate with the CAS endpoints. This can be easily simulated by writing the following keys into the etcd:
```bash
etcdctl mkdir /dogu/inttest
etcdctl set /dogu/inttest/0.0.1 '{“Name”: “official/inttest”, “Dependencies”:[“cas”]}'
etcdctl set /dogu/inttest/current “0.0.1”
```
There is now an “empty” dogu for which the CAS registers a service. This is used by the integration tests to communicate with the necessary endpoints. The name of the empty dogu must match the value for the `clientID` from the `cypress.json`.

**Step 2:**

In order for our OAuth tests to be successful, we need to create a service account in the CAS. We can also simulate this by storing a service account in etcd under the CAS path:
```bash
etcdctl set /config/cas/service_accounts/oauth/inttest “fda8e031d07de22bf14e552ab12be4bc70b94a1fb61cb7605833765cb74f2dea”
```
Here `inttest` must correspond to the name of the “empty” dogus from the first step. The value is the configured client secret from the `cypress.json` as SHA-256 hash.

**Step 3:**

In order for our tests for the Forgot password function to be carried out, we need to define a text in the CAS that is to be displayed when the Forgot password button is clicked.
A corresponding entry can be configured in etcd in the following way:

```bash
etcdctl set /config/cas/forgot_password_text 'Contact your admin'
```

The value expected by the tests is defined in `cypress.config.js` under the attribute `PasswordHintText`.

The password rules must also be set accordingly:
```bash
etcdctl set /config/_global/password-policy/must_contain_capital_letter true
etcdctl set /config/_global/password-policy/must_contain_lower_case_letter true
etcdctl set /config/_global/password-policy/must_contain_digit true
etcdctl set /config/_global/password-policy/must_contain_special_character true
etcdctl set /config/_global/password-policy/min_length 14
```

**Step 4**

In order for our tests to be carried out for the legal URLs such as the imprint, corresponding URLs must be defined in the CAS so that they are displayed in the footer.
Corresponding entries can be configured in etcd in the following way:

```bash
etcdctl set /config/cas/legal_urls/imprint 'https://cloudogu.com/'
etcdctl set /config/cas/legal_urls/privacy_policy 'https://www.triology.de/'
etcdctl set /config/cas/legal_urls/terms_of_service 'https://docs.cloudogu.com/'
```

The URLs expected by the tests are defined in `cypress.json` under the attributes `PrivacyPolicyURL`, `TermsOfServiceURL` and `ImprintURL`.

### Preparation: OIDC Provider

**Step 1:** Start Keycloak on the host machine and import realm (**Attention: The path to the JSON must be adjusted!**)

```bash
docker run --name kc -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin -p 9000:8080 -e KEYCLOAK_IMPORT="/realm-cloudogu.json -Dkeycloak.profile.feature. upload_scripts=enabled” -v /vagrant/containers/cas/integrationTests/keycloak-realm/realm-cloudogu.json:/realm-cloudogu.json quay.io/keycloak/keycloak:15.0.2
```

**Step 2:** Configuration for the CAS in the CES:

```bash
etcdctl set /config/cas/oidc/enabled “true”
etcdctl set /config/cas/oidc/discovery_uri “http://192.168.56.1:9000/auth/realms/Cloudogu/.well-known/openid-configuration”
etcdctl set /config/cas/oidc/client_id “casClient”
etcdctl set /config/cas/oidc/display_name “MyProvider”
etcdctl set /config/cas/oidc/optional “true”
etcdctl set /config/cas/oidc/scopes “openid email profile groups”
etcdctl set /config/cas/oidc/attribute_mapping “email:mail,family_name:surname,given_name:givenName,preferred_username:username,name:displayName”
```

**Step 3:** Use `cesapp edit-config cas` to set the oidc/client_secret to `c21a7690-1ca3-4cf9-bef3-22f37faf5144`. This will then be stored correctly encrypted.

## Starting the integration tests

The integration tests can be started in two ways:

1. with `yarn cypress run` the tests start only in the console without visual feedback.
   This mode is helpful if the focus is on execution.
   For example, with a Jenkins pipeline.

1. yarn cypress open` starts an interactive window where you can execute, visually observe and debug the tests.
   This mode is particularly helpful when developing new tests and finding errors.