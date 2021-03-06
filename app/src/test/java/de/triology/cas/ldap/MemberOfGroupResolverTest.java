/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.triology.cas.ldap;

import java.util.Arrays;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import org.jasig.cas.authentication.principal.Principal;
import org.junit.runner.RunWith;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link MemberOfGroupResolver}.
 * 
 * @author Sebastian Sdorra
 */
@RunWith(MockitoJUnitRunner.class)
public class MemberOfGroupResolverTest {

  @Mock
  private Principal principal;
  
  @Mock
  private LdapEntry ldapEntry;
  
  
  /**
   * Tests {@link MemberOfGroupResolver#resolveGroups(Principal, LdapEntry)} with memberUid attribute.
   */
  @Test
  public void testResolveGroupsWithMemberUid() {
    LdapAttribute attribute = mock(LdapAttribute.class);
    when(attribute.getStringValues()).thenReturn(Arrays.asList("a", "b", "c", "dc=d,o=d"));
    when(ldapEntry.getAttribute("memberUid")).thenReturn(attribute);
    
    MemberOfGroupResolver resolver = new MemberOfGroupResolver("memberUid", false);
    Set<String> groups = resolver.resolveGroups(principal, ldapEntry);
    assertThat(groups, containsInAnyOrder("a", "b", "c", "dc=d,o=d"));
  }
  
  /**
   * Tests {@link MemberOfGroupResolver#resolveGroups(Principal, LdapEntry)} with member attribute.
   */
  @Test
  public void testResolveGroupsWithMemberDn() {
    LdapAttribute attribute = mock(LdapAttribute.class);
    when(attribute.getStringValues()).thenReturn(Arrays.asList("cn=a", "cn=b,local", "c=c,c=c", "dc=d,o=d"));
    when(ldapEntry.getAttribute("member")).thenReturn(attribute);
    
    MemberOfGroupResolver resolver = new MemberOfGroupResolver("member", true);
    Set<String> groups = resolver.resolveGroups(principal, ldapEntry);
    assertThat(groups, containsInAnyOrder("a", "b", "c", "d"));
  }

}