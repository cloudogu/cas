const {
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");

When("the user clicks on the forgot password button", function () {
    cy.get('button[data-testid="login-forgot-password-button"]').click()
});

Then("CAS shows a hint text", function () {
    cy.get('p[data-testid="login-forgot-password-text"]').contains(Cypress.env('PasswordHintText'))
});