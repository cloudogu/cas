const {
    Then
} = require("cypress-cucumber-preprocessor/steps");

Then(/^a footer with three legal links is displayed$/, function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').get('[data-testId=footer-link-terms-of-service]').contains('Terms of Service').should('have.attr', 'href', Cypress.env("TermsOfServiceURL"))
    cy.get('footer').get('[data-testId=footer-link-legal-url-imprint]').contains('Imprint').should('have.attr', 'href', Cypress.env("ImprintURL"))
    cy.get('footer').get('[data-testId=footer-link-privacy-policy]').contains('Privacy Policy').should('have.attr', 'href', Cypress.env("PrivacyPolicyURL"))
});
