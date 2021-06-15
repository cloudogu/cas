const {
    Then
} = require("cypress-cucumber-preprocessor/steps");

Then(/^a footer with three legal links is displayed$/, function () {
    cy.get('footer').find('a').should('have.length', 3)
});
