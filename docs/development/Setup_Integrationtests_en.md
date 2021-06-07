# Setup for the integration tests

This section describes the steps required to properly run the integration tests.

## Requirements

* It is necessary to install the program `yarn

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
- `AdminGroup" - the user group for CES administrators.
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
      "ClientSecret" : "integrationTestClientSecret"
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

## Starting the integration tests

The integration tests can be started in two ways:

1. with `yarn cypress run` the tests start only in the console without visual feedback.
   This mode is useful when execution is the main focus.
   For example, in a Jenkins pipeline.
   
1. `yarn cypress open` starts an interactive window where you can run, visually observe and debug the tests.
   This mode is especially useful when developing new tests and finding bugs.
