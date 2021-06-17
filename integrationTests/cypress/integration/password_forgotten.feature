Feature: Simple test to check the forgotten password functionality of CAS

  Scenario: the user clicks on the forgot password button and is shown a hint text
    Given the user is currently not logged in the CES
    Given the browser shows the CAS login page
    When the user clicks on the forgot password button
    Then CAS shows a hint text