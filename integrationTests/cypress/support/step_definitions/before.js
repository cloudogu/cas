const {
    Before,
} = require("@badeball/cypress-cucumber-preprocessor");


/**
 * Create a new test user which must change his password at next login
 */
Before({tags: "@requires_testuser_who_must_change_pwd"}, () => {
    cy.fixture("testuser_data").then(function (testUser) {
        cy.usermgtTryDeleteUser(testUser.username)
        cy.log("Creating test user with pwdReset-attribute true")
        cy.usermgtCreateUser(testUser.username, testUser.givenname, testUser.surname, testUser.displayName, testUser.mail, testUser.password, true, testUser.groups)
    })
});
