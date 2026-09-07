package de.triology.cas.ldap.resolvers;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemberOfGroupResolverTest {

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry ldapEntry;

    @Test
    public void resolveGroupsWithMemberDn() {
        LdapAttribute attribute = mock(LdapAttribute.class);
        when(attribute.getStringValues()).thenReturn(Arrays.asList("cn=a", "cn=b,local", "c=c,c=c", "dc=d,o=d"));
        when(ldapEntry.getAttribute("member")).thenReturn(attribute);

        MemberOfGroupResolver resolver = new MemberOfGroupResolver("member");

        Set<String> groups = resolver.resolveGroups(principal, ldapEntry);
        assertThat(groups, containsInAnyOrder("a", "b", "c", "d"));
    }

}