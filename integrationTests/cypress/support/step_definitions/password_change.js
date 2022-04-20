const {
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");

Then("CAS prompts the test user to change his password", function () {
    cy.get("body").get("div[data-testid=\"login-reset-pw-msg\"]")
});

When("the test user changes his password", function () {
    cy.fixture("testuser_data").then(function (testUser) {
        cy.get('input[data-testid="password-input"]').type(testUser.newPassword)
        cy.get('input[data-testid="confirmedPassword-input"]').type(testUser.newPassword)
        cy.get('button[id="submit"]').click()
    })
});

Then("CAS confirms the successful change of the password", function () {
    cy.get(`h2[data-testid="h2-pwd-change-successful"]`)
});

When("the test user logs in with changed credentials", function () {
    cy.fixture("testuser_data").then(function (testuser) {
        cy.login(testuser.username, testuser.newPassword)
    })
});