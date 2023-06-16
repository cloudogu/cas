Feature: OAuth integration tests

  Scenario: registered service receives a service ticket when accessing the OAuth authorization endpoint
    Given the admin logs into the ces
    When a registered service requests the OAuth authorization endpoint
    Then a service ticket is returned

  Scenario: unregistered service receives an error when accessing the OAuth authorization endpoint
    When an unregistered service requests the OAuth authorization endpoint
    Then cas shows that the service is not authorized to access this endpoint

  Scenario: registered service exchanges service ticket for ticket granting ticket
    Given a valid service ticket is currently available
    When a registered service requests the OAuth accessToken endpoint
    Then a ticket granting ticket is returned

  Scenario: registered service requests accessToken without service ticket
    When a registered service requests the OAuth accessToken endpoint
    Then an invalid request respond is send

  Scenario: unregistered service requests accessToken with valid service ticket
    Given a valid service ticket is currently available
    When a unregistered service requests the OAuth accessToken endpoint
    Then an unauthorized json respond is send

  Scenario: registered service exchanges service ticket for ticket granting ticket
    Given a valid ticket granting ticket is currently available
    When a registered service requests the OAuth profile endpoint
    Then a profile is returned