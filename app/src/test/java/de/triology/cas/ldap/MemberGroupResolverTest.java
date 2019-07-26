package de.triology.cas.ldap;


import org.jasig.cas.authentication.principal.Principal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.SearchFilter;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MemberGroupResolverTest {

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry entry;

    @Test
    public void testSearchFilter() {
        when(principal.getId()).thenReturn("trillian");
        when(entry.getDn()).thenReturn("cn=Tricia,ou=People,dc=hitchhiker,dc=com");

        MemberGroupResolver resolver = new MemberGroupResolver();
        resolver.setSearchFilter("(&(objectClass=group)(member={1}))");

        SearchFilter filter = resolver.createFilter(principal, entry);
        assertEquals("cn=Tricia,ou=People,dc=hitchhiker,dc=com", filter.getParameters().get("0"));
        assertEquals("trillian", filter.getParameters().get("1"));
    }

}
