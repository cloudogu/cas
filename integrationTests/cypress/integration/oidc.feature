Feature: Simple tests to check the OIDC provider authentication delegation

  Scenario: the user visits the cas login mask that shows a button to login via oidc provider
    Given the user is currently not logged in the CES
    When the browser shows the CAS login page
    Then a button for delegated OIDC authentication is shown