Feature: Simple test to check the forgotten password functionality of cas

  Scenario: the user successfully logs into the CES with admin credentials and CAS shows the profile page
    Given the user is currently not logged in the CES
    Given the browser shows the cas login page
    When the user clicks on the forgot password button
    Then cas shows a hint text