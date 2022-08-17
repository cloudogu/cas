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

When("the user enters an invalid password", function () {
    cy.get('input[data-testid="password-input"]').type("invalid_pwd")
    cy.get('input[data-testid="confirmedPassword-input"]').type("invalid_pwd")
});

Then("CAS displays a notice of an invalid password", function () {
    cy.get('div[data-testid=password-policy-violation-msg-div').should('be.visible')
})

Then("CAS displays the password policy criteria", function () {
    cy.get('li[data-testid=pwd-rule-capital-letter-li').should('be.visible')
    cy.get('li[data-testid=pwd-rule-lower-case-letter-li').should('be.visible')
    cy.get('li[data-testid=pwd-rule-digit-li').should('be.visible')
    cy.get('li[data-testid=pwd-rule-special-character-li').should('be.visible')
    cy.get('li[data-testid=pwd-rule-min-length').should('be.visible')
})