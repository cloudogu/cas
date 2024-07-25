const {
    Given,
    When,
    Then
} = require("@badeball/cypress-cucumber-preprocessor");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

const CasServiceTicketPattern = "OC-[0-9]*-[a-zA-Z0-9-]*"
let latestOAuthCode = ""
let latestAccessToken = ""
let latestProfile = ""
let latestBody = ""
let latestStatus = undefined

function resetData() {
    latestOAuthCode = ""
    latestAccessToken = ""
    latestProfile = ""
    latestBody = ""
    latestStatus = undefined
}

function serviceRequestsAuthorizationEndpoint(clientID, failOnErrorCode = false) {
    cy.getOAuth20Authorize(clientID, failOnErrorCode).then(function (window) {
        let href = window.location.href
        cy.log("###### HREF ######", href)
        latestOAuthCode = href.match(CasServiceTicketPattern)
    })
}

function serviceRequestsAccessTokenEndpoint(clientID, serviceTicket, failOnErrorCode = false) {
    cy.log("serviceRequestsAccessTokenEndpoint with service-ticket: " + serviceTicket)
    cy.getOAuth20AccessToken(clientID, serviceTicket, failOnErrorCode).then(function (response) {
        latestBody = response.body
        latestStatus = response.status
        latestAccessToken = latestBody.access_token
    })
}

function serviceRequestsProfileEndpoint(ticketGrantingTicket) {
    cy.getOAuth20Profile(ticketGrantingTicket, false).then(function (response) {
        latestBody = response.body
        latestProfile = latestBody
    })
}

function casAdminLogin() {
    cy.visit("/cas/logout")
    cy.visit("/cas/login")
    cy.clickWarpMenuCheckboxIfPossible()

    cy.get('input[data-testid=login-username-input-field]').type(env.GetAdminUsername())
    cy.get('input[data-testid=login-password-input-field]').type(env.GetAdminPassword())
    cy.get('div[data-testid=login-form-login-button-container]').children('button').click()
}

Given("the admin logs into the ces", function () {
    casAdminLogin()
});

Given("a valid service ticket is currently available", function () {
    casAdminLogin()
    serviceRequestsAuthorizationEndpoint(Cypress.env("ClientID"))
});

Given("a valid ticket granting ticket is currently available", function () {
    casAdminLogin()
    cy.getOAuth20Authorize(Cypress.env("ClientID"), false).then(function (response) {
        let href = response.location.href
        latestOAuthCode = href.match(CasServiceTicketPattern)
        serviceRequestsAccessTokenEndpoint(Cypress.env("ClientID"), latestOAuthCode, true)
    })
});

When("a registered service requests the OAuth authorization endpoint", function () {
    serviceRequestsAuthorizationEndpoint(Cypress.env("ClientID"))
});

When("an unregistered service requests the OAuth authorization endpoint", function () {
    serviceRequestsAuthorizationEndpoint("unregisteredServiceClientID", false)
});

When("a registered service requests the OAuth accessToken endpoint", function () {
    serviceRequestsAccessTokenEndpoint(Cypress.env("ClientID"), latestOAuthCode, false)
});

When("a unregistered service requests the OAuth accessToken endpoint", function () {
    serviceRequestsAccessTokenEndpoint("unregisteredServiceClientID", latestOAuthCode, false)
});

When("a registered service requests the OAuth profile endpoint", function () {
    serviceRequestsProfileEndpoint(latestAccessToken)
});

Then("a service ticket is returned", function () {
    assert(latestOAuthCode.toString() !== "", "Service Ticket should not be empty")
    assert(latestOAuthCode.toString().match(CasServiceTicketPattern), "Service Ticket should match OC-Pattern")
    resetData()
});

Then("a ticket granting ticket is returned", function () {
    assert(latestAccessToken.toString() !== "", "AT should not be empty")
    resetData()
});

Then("a profile is returned", function () {
    assert(latestProfile.toString() !== "", "Profile should not be empty")
    assert(latestProfile.id === env.GetAdminUsername(), "Profile should contain the correct username")
    assert(latestProfile.attributes.username === env.GetAdminUsername(), "Profile should contain the correct username")
    assert(JSON.stringify(latestProfile.attributes.groups).includes("cesManager"), "Profile should contain the cesManager group")
    assert(JSON.stringify(latestProfile.attributes.groups).includes(env.GetAdminGroup()), "Profile should contain the admin group")
    resetData()
});

Then("cas shows that the service is not authorized to access this endpoint", function () {
    // This header element comes from the vanilla CAS therefore this component doesn't implement the test-key convention
    // and is select by h2
    cy.get('h2').contains("Application Not Authorized to Use CAS")
    resetData()
});

Then("an invalid request respond is send", function () {
    assert(JSON.stringify(latestBody) === '{"error":"invalid_grant"}', "body should contain invalid request")
    resetData()
});

Then("an unauthorized json respond is send", function () {
    assert(latestStatus === 401, "body should have 401 status code")
    resetData()
});
