Feature: Tests for the behaviour when a user has to change his password

  @requires_testuser_who_must_change_pwd
  Scenario: a user who needs to change his password logs in and changes his password
    Given the user is currently not logged in the CES
    When the user opens the dogu start page
    And the test user logs in with correct credentials
    Then CAS prompts the test user to change his password
    When the test user changes his password
    Then CAS confirms the successful change of the password
    When the user opens the dogu start page
    And the test user logs in with changed credentials
    Then CAS shows the profile page of the user "testuser" with the user ID from LDAP entry

  @requires_testuser_who_must_change_pwd
  Scenario: a user cannot change his password if the password entered does not comply with the password policy
    Given the user is currently not logged in the CES
    When the user opens the dogu start page
    And the test user logs in with correct credentials
    Then CAS prompts the test user to change his password
    When the user enters an invalid password
    Then CAS displays a notice of an invalid password
    And  CAS displays the password policy criteria