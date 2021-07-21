package de.triology.cas.ldap;


import org.apereo.cas.authentication.principal.Principal;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.FilterTemplate;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MemberGroupResolverTest {

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry entry;

    @Before
    public void setUp() {
        when(principal.getId()).thenReturn("trillian");
        when(entry.getDn()).thenReturn("cn=Tricia,ou=People,dc=hitchhiker,dc=com");
    }

    @Test
    public void testSearchFilterWithPrincipalId() {
        var resolver = new MemberGroupResolver(null, null, null, "(&(objectClass=posixGroup)(memberUid={1}))");
        assertFilter(resolver,"(&(objectClass=posixGroup)(memberUid=trillian))");
    }

    private void assertFilter( MemberGroupResolver resolver, String expected) {
        FilterTemplate filter = resolver.createFilter(principal, entry);
        assertEquals(expected, filter.format());
    }

    @Test
    public void testSearchFilterWithPrincipalDN() {
        var resolver = new MemberGroupResolver(null, null, null, "(&(objectClass=inetOrgPerson)(member={0}))");
        assertFilter(resolver,"(&(objectClass=inetOrgPerson)(member=cn=Tricia,ou=People,dc=hitchhiker,dc=com))");
    }
}
