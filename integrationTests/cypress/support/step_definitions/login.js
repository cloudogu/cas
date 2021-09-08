const {
    Given,
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

Given(/^the browser shows the CAS login page$/, function () {
    cy.visit("/cas/login")
    cy.clickWarpMenuCheckboxIfPossible()
});

Given(/^the user is currently not logged in the CES$/, function () {
    cy.visit("/cas/logout")
});

When(/^the user logs into the CES with the admin credentials$/, function () {
    cy.clickWarpMenuCheckboxIfPossible()
    cy.login(env.GetAdminUsername(), env.GetAdminPassword());
});

Then(/^CAS shows the profile page of the user$/, function () {
    cy.get('h2').contains("Log In Successful")
    cy.get(':nth-child(2) > strong').contains(env.GetAdminUsername())
});
