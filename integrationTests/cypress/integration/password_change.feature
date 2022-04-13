Feature: Tests for the behaviour when a user has to change his password

  @requires_testuser_who_must_change_pwd
  Scenario: a user who needs to change his password logs in and changes his password
    Given the user is currently not logged in the CES
    When the user opens the dogu start page
    And the test user logs in with correct credentials