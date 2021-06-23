/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.triology.cas.ldap;

import org.apache.commons.collections.CollectionUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

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
    Map<String,List<Object>> attributes = new HashMap<>();
    attributes.put("user", Collections.singletonList("trillian"));
    
    Set<String> groups = new HashSet<>(Arrays.asList("a", "b"));
    
    when(principal.getId()).thenReturn("tricia");
    when(principal.getAttributes()).thenReturn(attributes);
    when(resolver.resolveGroups(principal, ldapEntry)).thenReturn(groups);
    
    handler.setGroupResolver(resolver);
    
    Principal principalWithGroups = handler.attachGroups(principal, ldapEntry);
    assertNotNull(principalWithGroups);
    assertNotSame(principalWithGroups, principal);
    
    assertEquals(new ArrayList<>(groups), principalWithGroups.getAttributes().get("groups"));
  }

}