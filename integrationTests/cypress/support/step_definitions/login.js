const {
    Given,
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

Given("the browser shows the CAS login page", function () {
    cy.visit("/cas/login")
    cy.clickWarpMenuCheckboxIfPossible()
});

Given("the user is currently not logged in the CES", function () {
    cy.visit("/cas/logout")
});

Given("the browser shows the CAS login page and the browser language is set to German", function () {
    cy.visit("/cas/login", {
        onBeforeLoad(window) {
            Object.defineProperty(window.navigator, 'language', {value: 'de-DE'});
            Object.defineProperty(window.navigator, 'languages', {value: ['de-DE']});
            Object.defineProperty(window.navigator, 'accept_languages', {value: ['de']});
        },
        headers: {
            'Accept-Language': 'de'
        },
    });
    cy.clickWarpMenuCheckboxIfPossible()
})

When("the user logs into the CES with the admin credentials", function () {
    cy.clickWarpMenuCheckboxIfPossible()
    cy.login(env.GetAdminUsername(), env.GetAdminPassword());
});

When("When the user logs into the CES with the admin credentials, writing the username in capital letters", function () {
    cy.clickWarpMenuCheckboxIfPossible()
    cy.login(env.GetAdminUsername().toUpperCase(), env.GetAdminPassword());
});

Then("CAS shows the profile page of the user with the user ID from LDAP entry", function () {
    cy.get('h2[data-testid=login-header]').contains("Log In Successful")
    cy.get(':nth-child(2) > strong').contains(env.GetAdminUsername())
});
