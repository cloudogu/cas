Feature: Simple test to check the login functionality of cas

  Scenario: registered service receives a service ticket when accessing the OAuth authorization endpoint
    Given the user is currently not logged in the CES
    Given the browser shows the cas login page
    When the user logs into the CES with the admin credentials
    Then cas shows the profile page of the user