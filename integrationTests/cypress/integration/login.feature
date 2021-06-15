Feature: Simple test to check the login functionality of cas

  Scenario: the user successfully logs into the CES with admin credentials and CAS shows the profile page
    Given the user is currently not logged in the CES
    Given the browser shows the cas login page
    When the user logs into the CES with the admin credentials
    Then cas shows the profile page of the user