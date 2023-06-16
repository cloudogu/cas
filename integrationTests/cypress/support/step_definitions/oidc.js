const {
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

Then("a button for delegated OIDC authentication is shown", function () {
    cy.get('[data-testid=login-provider-oidc]')
});
