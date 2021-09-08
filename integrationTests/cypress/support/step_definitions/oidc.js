const {
    Then
} = require("cypress-cucumber-preprocessor/steps");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

Then(/^a button for delegated OIDC authentication is shown$/, function () {
    cy.get('#MyProvider').contains("MyProvider")
});
