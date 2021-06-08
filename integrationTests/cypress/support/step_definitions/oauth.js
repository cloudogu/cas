const {
    Given,
    When,
    Then
} = require("cypress-cucumber-preprocessor/steps");
const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables')

const CasServiceTicketPattern = "ST-[0-9]*-[a-zA-Z0-9-]*-cas"
let latestServiceTicket = ""
let latestTicketGrantingTicket = ""
let latestProfile = ""
let latestBody = ""

function resetData() {
    latestServiceTicket = ""
    latestTicketGrantingTicket = ""
    latestProfile = ""
    latestBody = ""
}

function serviceRequestsAuthorizationEndpoint(clientID) {
    cy.getOAuth20Authorize(clientID, true).then(function (response) {
        latestBody = response.body
        latestServiceTicket = latestBody.match(CasServiceTicketPattern)
        console.log("Set ST to : " + latestServiceTicket)
    })
}

function serviceRequestsAccessTokenEndpoint(clientID, serviceTicket, failOnErrorCode = false) {
    cy.getOAuth20AccessToken(clientID, serviceTicket, failOnErrorCode).then(function (response) {
        latestBody = response.body
        latestTicketGrantingTicket = latestBody.access_token
        console.log("Set TGT to : " + latestTicketGrantingTicket)
    })
}

function serviceRequestsProfileEndpoint(ticketGrantingTicket) {
    cy.getOAuth20Profile(ticketGrantingTicket, true).then(function (response) {
        latestBody = response.body
        latestProfile = latestBody
        console.log("Set profile to : " + JSON.stringify(latestProfile))
    })
}

Given(/^the admin logs into the ces$/, function () {
    cy.loginAdmin()
});

Given(/^a valid service ticket is currently available$/, function () {
    cy.loginAdmin()
    serviceRequestsAuthorizationEndpoint(Cypress.env("ClientID"))
});

Given(/^a valid ticket granting ticket is currently available$/, function () {
    cy.loginAdmin()
    cy.getOAuth20Authorize(Cypress.env("ClientID"), true).then(function (response) {
        latestServiceTicket = response.body.match(CasServiceTicketPattern)
        console.log("Set ST to : " + latestServiceTicket)
        serviceRequestsAccessTokenEndpoint(Cypress.env("ClientID"), latestServiceTicket, true)
    })
});

When(/^a registered service requests the OAuth authorization endpoint$/, function () {
    serviceRequestsAuthorizationEndpoint(Cypress.env("ClientID"))
});

When(/^an unregistered service requests the OAuth authorization endpoint$/, function () {
    cy.visit(Cypress.config().baseUrl + "/cas/oauth2.0/authorize", {
        qs: {
            client_id: 'notRegisteredService',
            redirect_uri: 'https://oauthdebugger.com/debug',
            response_type: 'code',
            state: '673bac67-cb29-47b4-beed-dc26aa70eaeb',
        },
        retryOnStatusCodeFailure: false
    })
});

When(/^a registered service requests the OAuth accessToken endpoint$/, function () {
    serviceRequestsAccessTokenEndpoint(Cypress.env("ClientID"), latestServiceTicket, false)
});

When(/^a unregistered service requests the OAuth accessToken endpoint$/, function () {
    serviceRequestsAccessTokenEndpoint("unregisteredServiceClientID", latestServiceTicket, false)
});

When(/^a registered service requests the OAuth profile endpoint$/, function () {
    serviceRequestsProfileEndpoint(latestTicketGrantingTicket)
});

Then(/^a service ticket is returned$/, function () {
    assert(latestServiceTicket.toString() !== "", "Service Ticket should not be empty")
    assert(latestServiceTicket.toString().match(CasServiceTicketPattern), "Service Ticket should match ST-Pattern")
    resetData()
});

Then(/^a ticket granting ticket is returned$/, function () {
    assert(latestTicketGrantingTicket.toString() !== "", "TGT should not be empty")
    resetData()
});

Then(/^a profile is returned$/, function () {
    console.log(JSON.stringify(latestProfile.attributes.groups))
    assert(latestProfile.toString() !== "", "Profile should not be empty")
    assert(latestProfile.id === env.GetAdminUsername(), "Profile should contain the correct username" )
    assert(latestProfile.attributes.username === env.GetAdminUsername(), "Profile should contain the correct username" )
    assert(JSON.stringify(latestProfile.attributes.groups).includes("cesManager"), "Profile should contain the cesManager group" )
    assert(JSON.stringify(latestProfile.attributes.groups).includes(env.GetAdminGroup()), "Profile should contain the admin group" )
    resetData()
});

Then(/^cas shows that the service is not authorized to access this endpoint$/, function () {
    cy.get('h2').contains("Application Not Authorized to Use CAS")
    resetData()
});

Then(/^an invalid request respond is send$/, function () {
    assert(latestBody.toString() === "error=invalid_request", "body should contain invalid request")
    resetData()
});