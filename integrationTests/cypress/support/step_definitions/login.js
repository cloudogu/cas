const {
    Given,
    When,
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

Given("the browser shows the CAS login page", function () {
    cy.visit("/cas/login")
    cy.clickWarpMenuCheckboxIfPossible()
});

Given("the user is currently not logged in the CES", function () {
    cy.visit("/cas/logout")
});

Given("the browser shows the CAS login page and the browser language is set to German", function () {
    cy.visit("/cas/login?locale=de")
    cy.clickWarpMenuCheckboxIfPossible()
})

When("the user logs into the CES with the admin credentials", function () {
    cy.clickWarpMenuCheckboxIfPossible()
    cy.login(env.GetAdminUsername(), env.GetAdminPassword());
});

When("the user logs into the CES with the admin credentials, writing the username in capital letters", function () {
    cy.clickWarpMenuCheckboxIfPossible()
    cy.login(env.GetAdminUsername().toUpperCase(), env.GetAdminPassword());
});

Then("CAS shows the profile page of the user {string} with the user ID from LDAP entry", function (username) {
    cy.get('h1[data-testid=login-header]').contains("Log In Successful")
});

When("the user enters a password {string}", function (password) {
    cy.get('input[data-testid=login-password-input-field').type(password)
})

When("clicks the checkbox for showing the password", function () {
    cy.get('input[data-testid=password-reveal-checkbox').click()
})

Then("the password field shows {string}", function () {
    cy.get('input[data-testid=password-reveal-checkbox').should('have.attr', 'checked')
    cy.get('input[data-testid=login-password-input-field').should('have.attr', 'type', 'text')
})