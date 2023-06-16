const {
    When,
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require("@cloudogu/dogu-integration-test-library/lib/environment_variables");

When("the user clicks on the reset password link", function () {
    cy.get('button[data-testid="reset-password-button"]').click()
})

Then("the user is taken to a password reset page", function () {
    cy.get('h3[data-testid="password-reset-header"').should('be.visible')
})

When("the user enters his username on the password reset page", function () {
    cy.get('input[data-testid="password-reset-username-input"]').type(env.GetAdminUsername())
});

When("the user submits his input on the password reset page", function () {
    cy.get('button[type="submit"]').click()
});

Then("CAS shows a confirmation page about a sent email", function () {
    let browserLanguage = navigator.language
    cy.log("browser language is " + browserLanguage)

    if (browserLanguage.startsWith("en") ) {
        cy.get("h2").contains("Password Reset Instructions Sent Successfully")
    } else if (browserLanguage.startsWith("de")) {
        console.log(browserLanguage);
        cy.get("h2").contains("Anweisungen zum Zurücksetzen des Passworts erfolgreich versandt")
    } else {
        throw new Error("Test fails, unknown browser language")
    }
});

When("the user enters an not existing username on the password reset page", function () {
    let notExistingUsername="Dustin1234qwertz"
    cy.get('input[data-testid="password-reset-username-input"]').type(notExistingUsername)
})
