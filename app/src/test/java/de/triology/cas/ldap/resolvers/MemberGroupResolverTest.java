package de.triology.cas.ldap.resolvers;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.FilterTemplate;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MemberGroupResolverTest {

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry entry;

    @BeforeEach
    public void setUp() {
        // lenient: not every test method exercises both stubs
        lenient().when(principal.getId()).thenReturn("trillian");
        lenient().when(entry.getDn()).thenReturn("cn=Tricia,ou=People,dc=hitchhiker,dc=com");
    }

    @Test
    public void searchFilterWithPrincipalId() {
        var resolver = new MemberGroupResolver(null, null, "(&(objectClass=posixGroup)(memberUid={1}))");
        assertFilter(resolver, "(&(objectClass=posixGroup)(memberUid=trillian))");
    }

    private void assertFilter(MemberGroupResolver resolver, String expected) {
        FilterTemplate filter = resolver.createFilter(principal, entry);
        assertEquals(expected, filter.format());
    }

    @Test
    public void searchFilterWithPrincipalDN() {
        var resolver = new MemberGroupResolver(null, null, "(&(objectClass=inetOrgPerson)(member={0}))");
        assertFilter(resolver, "(&(objectClass=inetOrgPerson)(member=cn=Tricia,ou=People,dc=hitchhiker,dc=com))");
    }
}
