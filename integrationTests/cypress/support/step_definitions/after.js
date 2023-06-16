const {
    After,
} = require("@badeball/cypress-cucumber-preprocessor");

module.exports.register = function () {
    /**
     * Deletes the created test user after every scenario
     */
    After({tags: "@requires_testuser_who_must_change_pwd"}, () => {
        cy.logout();

        cy.fixture("testuser_data").then(function (testUser) {
            cy.log("Removing test user")
            cy.usermgtDeleteUser(testUser.username)
        })
    });
}