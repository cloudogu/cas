const env = require('@cloudogu/dogu-integration-test-library/lib/environment_variables');

/**
 * Makes a GET request to the OAuth 2.0 authorize endpoint of the CAS.
 * @param {String} clientID - The client id to use for the request.
 * @param {boolean} exitOnFail - Determines whether the test should fail when the request did not succeed. Default: false
 * @returns The response of the request.
 */
const getOAuth20Authorize = (clientID, exitOnFail = false) => {
    cy.request({
        method: "GET",
        url: Cypress.config().baseUrl + "/cas/oauth2.0/authorize",
        qs: {
            client_id: clientID,
            redirect_uri: 'https://oauthdebugger.com/debug',
            response_type: 'code',
            state: '673bac67-cb29-47b4-beed-dc26aa70eaeb',
        },
        failOnStatusCode: exitOnFail
    })
}

/**
 * Makes a GET request to the OAuth 2.0 access token endpoint of the CAS to exchange a service ticket for a ticket granting ticket.
 * @param {String} clientID - The client id to use for the request.
 * @param {String} accessToken - The valid service ticket that should be exchanged for the ticket granting ticket.
 * @param {boolean} exitOnFail - Determines whether the test should fail when the request did not succeed. Default: false
 * @returns The response of the request.
 */
const getOAuth20AccessToken = (clientID, accessToken, exitOnFail = false) => {
    console.log("AccessToken: " + accessToken)
    cy.request({
        method: "GET",
        url: Cypress.config().baseUrl + "/cas/oauth2.0/accessToken",
        qs: {
            grant_type: 'authorization_code',
            code: accessToken.toString(),
            client_id: clientID,
            redirect_uri: Cypress.config().baseUrl + "/" + Cypress.env("ClientID"),
            client_secret: Cypress.env("ClientSecret"),
        },
        failOnStatusCode: exitOnFail
    })
}

/**
 * Makes a GET request to the OAuth 2.0 profile endpoint of the CAS to get the profile with a valid ticket granting ticket.
 * @param {String} ticketGrantingTicket - The ticket granting ticket to use as the 'Bearer' token for the request.
 * @param {boolean} exitOnFail - Determines whether the test should fail when the request did not succeed. Default: false
 * @returns The response of the request.
 */
const getOAuth20Profile = (ticketGrantingTicket, exitOnFail = false) => {
    cy.request({
        method: "GET",
        url: Cypress.config().baseUrl + "/cas/oauth2.0/profile",
        headers: {
            "Content-Type": 'application/json',
            "Authorization": 'Bearer ' + ticketGrantingTicket
        },
        failOnStatusCode: false
    })
}

// cas/oauth2.0/authorize
Cypress.Commands.add("getOAuth20Authorize", getOAuth20Authorize)
// cas/oauth2.0/accessToken
Cypress.Commands.add("getOAuth20AccessToken", getOAuth20AccessToken)
// cas/oauth2.0/profile
Cypress.Commands.add("getOAuth20Profile", getOAuth20Profile)

