const {
    Then
} = require("@badeball/cypress-cucumber-preprocessor");

Then("a footer with three legal links is displayed", function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').get('[data-testId=footer-link-terms-of-service]').contains('Terms of Service').should('have.attr', 'href', Cypress.env("TermsOfServiceURL"))
    cy.get('footer').get('[data-testId=footer-link-legal-url-imprint]').contains('Imprint').should('have.attr', 'href', Cypress.env("ImprintURL"))
    cy.get('footer').get('[data-testId=footer-link-privacy-policy]').contains('Privacy Policy').should('have.attr', 'href', Cypress.env("PrivacyPolicyURL"))
});


Then("the legal links in the footer are translated to German", function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').get('[data-testId=footer-link-terms-of-service]').contains('Nutzungsbedingungen')
    cy.get('footer').get('[data-testId=footer-link-legal-url-imprint]').contains('Impressum')
    cy.get('footer').get('[data-testId=footer-link-privacy-policy]').contains('Datenschutzerklärung')
});

Then("the reset password button is translated to German", function () {
    cy.get('button[data-testid="reset-password-button"]').contains('Passwort zurücksetzen')
});