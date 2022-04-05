const {
    Then, Given
} = require("cypress-cucumber-preprocessor/steps");

Then(/^a footer with three legal links is displayed$/, function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').get('[data-testId=footer-link-terms-of-service]').contains('Terms of Service').should('have.attr', 'href', Cypress.env("TermsOfServiceURL"))
    cy.get('footer').get('[data-testId=footer-link-legal-url-imprint]').contains('Imprint').should('have.attr', 'href', Cypress.env("ImprintURL"))
    cy.get('footer').get('[data-testId=footer-link-privacy-policy]').contains('Privacy Policy').should('have.attr', 'href', Cypress.env("PrivacyPolicyURL"))
});


Then(/^the legal links in the footer are translated to german$/, function () {
    cy.get('footer').find('a').should('have.length', 3)

    cy.get('footer').get('[data-testId=footer-link-terms-of-service]').contains('Nutzungsbedingungen')
    cy.get('footer').get('[data-testId=footer-link-legal-url-imprint]').contains('Impressum')
    cy.get('footer').get('[data-testId=footer-link-privacy-policy]').contains('Datenschutzerklärung')
});

Then(/^the forgot password button is translated to german$/, function () {
    cy.get('button[data-testid="login-forgot-password-button"]').contains('Passwort vergessen?')
});