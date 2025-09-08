const {
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')


Then("a button for delegated OIDC authentication is shown", function () {
    cy.get('[data-testid=login-provider-oidc]').click()
});

When("the user enters keycloak username", function () {
    cy.get('input[id=username]').type("tester")
});

When("the user enters keycloak password", function () {
    cy.get('input[id=password]').type("test")
});

Then("login to keycloak", function () {
    cy.get('[id=kc-login]').click()
});

Then("CAS shows successful login", function () {
    cy.get('h1[data-testid=login-header]').contains("Log In Successful")
});