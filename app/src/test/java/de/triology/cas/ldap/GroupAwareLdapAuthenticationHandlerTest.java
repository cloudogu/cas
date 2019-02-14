/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.triology.cas.ldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jasig.cas.authentication.principal.Principal;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link GroupAwareLdapAuthenticationHandler}.
 * 
 * @author Sebastian Sdorra
 */
@RunWith(MockitoJUnitRunner.class)
public class GroupAwareLdapAuthenticationHandlerTest {

  @Mock
  private Authenticator authenticator;

  @Mock
  private GroupResolver resolver;
  
  @InjectMocks
  private GroupAwareLdapAuthenticationHandler handler;
  
  @Mock
  private Principal principal;
  
  @Mock
  private LdapEntry ldapEntry;
  
  /**
   * Tests {@link GroupAwareLdapAuthenticationHandler#attachGroups(Principal, LdapEntry) }.
   */
  @Test
  public void testAttachGroups() {
    Map<String,Object> attributes = new HashMap<>();
    attributes.put("user", "trillian");
    
    Set<String> groups = new HashSet<>(Arrays.asList("a", "b"));
    
    when(principal.getId()).thenReturn("tricia");
    when(principal.getAttributes()).thenReturn(attributes);
    when(resolver.resolveGroups(principal, ldapEntry)).thenReturn(groups);
    
    handler.setGroupResolver(resolver);
    
    Principal principalWithGroups = handler.attachGroups(principal, ldapEntry);
    assertNotNull(principalWithGroups);
    assertNotSame(principalWithGroups, principal);
    
    assertSame(groups, principalWithGroups.getAttributes().get("groups"));
  }

}