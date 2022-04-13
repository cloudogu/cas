// Loads all steps from the dogu integration library into this project
const doguTestLibrary = require('@cloudogu/dogu-integration-test-library')
doguTestLibrary.registerSteps()

When("the user clicks the dogu logout button", function () {
    // a ui button does not exist -> use backlog
    cy.visit("/cas/logout")
});

Then("the user has administrator privileges in the dogu", function () {
    // the cas 6 shows the groups in the ui, however not all groups are shown -> not feasible to detect -> skip
});

Then("the user has no administrator privileges in the dogu", function () {
    // the cas 6 shows the groups in the ui, however not all groups are shown -> not feasible to detect -> skip
});
