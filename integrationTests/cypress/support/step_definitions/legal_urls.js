const {
    Then
} = require("cypress-cucumber-preprocessor/steps");

Then(/^a footer with three legal links is displayed$/, function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').contains('Terms of Service').should('have.attr', 'href', Cypress.env("TermsOfServiceURL"))
    cy.get('footer').contains('Imprint').should('have.attr', 'href', Cypress.env("ImprintURL"))
    cy.get('footer').contains('Privacy Policy').should('have.attr', 'href', Cypress.env("PrivacyPolicyURL"))
});
