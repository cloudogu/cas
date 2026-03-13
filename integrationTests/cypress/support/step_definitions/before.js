const {
    Before,
} = require("@badeball/cypress-cucumber-preprocessor");


/**
 * Skip scenarios tagged with @skip_on_multinode when running in a multi-node pipeline.
 * Set the Cypress env variable "multiNode" to true to activate this behaviour.
 * This should only be used for tests which are not YET compatible with the multi-node pipeline, but will be adapted in
 * the future.
 */
Before({tags: "@skip_on_multinode"}, function () {
    if (Cypress.env("multiNode") === true) {
        this.skip();
    }
});

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
