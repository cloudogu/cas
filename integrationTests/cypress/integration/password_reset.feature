Feature: Simple tests for the password reset function of the CAS

  # Notes:
  # -Although the functionality itself is provided by the CAS, this is individually configured for the CES and also modified.
  # -The tests end with the sending of the e-mail, as it is too time-consuming and, above all, there are too many dependencies
  # on other dogus (e.g. Postfix) to also be able to test the success of the e-mail.

  Scenario: a user has an email sent to him via CAS to reset his password
    Given the user is currently not logged in the CES
    Given the browser shows the CAS login page
    When the user clicks on the reset password link
    Then the user is taken to a password reset page
    When the user enters his username on the password reset page
    And  the user submits his input on the password reset page
    Then CAS shows a confirmation page about a sent email

  # Note: The password reset behaviour is the same for existing and non-existing users.
  Scenario: an unknown user tries to reset his password
    Given the user is currently not logged in the CES
    Given the browser shows the CAS login page
    When the user clicks on the reset password link
    Then the user is taken to a password reset page
    When the user enters an not existing username on the password reset page
    And  the user submits his input on the password reset page
    Then CAS shows a confirmation page about a sent email