/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.ldap;

import junit.framework.TestCase;

/**
 *
 * @author Sebastian Sdorra
 */
public class AdvancedLdapAuthenticationHandlerTest extends TestCase
{
  
  public AdvancedLdapAuthenticationHandlerTest(String testName)
  {
    super(testName);
  }

  public void testSomeMethod()
  {
    assertEquals(
      "UniverseAdministrators", 
      AdvancedLdapAuthenticationHandler.fixRDNValue(
        "cn=UniverseAdministrators,ou=Groups,o=ces.local,dc=cloudogu,dc=com"
      )
    );
  }
  
}
