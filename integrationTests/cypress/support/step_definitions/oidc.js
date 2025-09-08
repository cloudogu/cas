const {
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')


Given("the user is currently not logged in the CES", function () {
    cy.visit("/cas/logout")
});

Given("the browser shows the CAS login page", function () {
    cy.visit("/cas/login")
    cy.clickWarpMenuCheckboxIfPossible()
});

Then("a button for delegated OIDC authentication is shown", function () {
    cy.get('[data-testid=login-provider-oidc]').click()
});

When("the user enters keycloak username", function () {
    cy.get('input[id=username]').type("tester")
})

When("the user enters keycloak password", function () {
    cy.get('input[id=password]').type("test")
})

Then("login to keycloak", function () {
    cy.get('[id=kc-login]').click()
});