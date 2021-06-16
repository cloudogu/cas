const {
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");

When(/^the user clicks on the forgot password button$/, function () {
    cy.get('button[id="forgotPassword"]').click()
});

Then(/^CAS shows a hint text$/, function () {
    cy.get('p[id="forgotPasswordInfo"]').contains(Cypress.env('PasswordHintText'))
});