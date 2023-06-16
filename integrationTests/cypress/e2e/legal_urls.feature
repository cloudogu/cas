Feature: Simple test to check if the links for imprint, privacy policy and terms of service are displayed

  Scenario: the user opens the CAS login page and the links for imprint, privacy policy and terms of service are displayed
    Given the user is currently not logged in the CES
    When the browser shows the CAS login page
    Then a footer with three legal links is displayed

  Scenario: the user opens the CAS login page and legal links aswell as button texts are correctly translated
    Given the user is currently not logged in the CES
    When the browser shows the CAS login page and the browser language is set to German
    Then the legal links in the footer are translated to German
    And the reset password button is translated to German