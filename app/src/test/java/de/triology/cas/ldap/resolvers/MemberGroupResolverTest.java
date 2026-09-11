package de.triology.cas.ldap.resolvers;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.FilterTemplate;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResponse;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockConstruction;
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

    @Test
    public void resolveGroups_missingSearchFilter_skipsLookupAndReturnsEmptySet() {
        // Uses fresh, unstubbed mocks (not the shared @Before ones): the missing-filter branch
        // returns before touching principal/ldapEntry at all, so nothing needs stubbing here.
        Principal freshPrincipal = mock(Principal.class);
        LdapEntry freshEntry = mock(LdapEntry.class);

        var resolver = new MemberGroupResolver("baseDN", mock(ConnectionFactory.class), null);
        assertThat(resolver.resolveGroups(freshPrincipal, freshEntry), empty());

        var resolverWithBlankFilter = new MemberGroupResolver("baseDN", mock(ConnectionFactory.class), "");
        assertThat(resolverWithBlankFilter.resolveGroups(freshPrincipal, freshEntry), empty());
    }

    @Test
    public void resolveGroups_success_aggregatesGroupNamesFromEntries() {
        LdapEntry groupOne = new LdapEntry();
        groupOne.addAttributes(new LdapAttribute("cn", "Admins"));
        LdapEntry groupTwo = new LdapEntry();
        groupTwo.addAttributes(new LdapAttribute("cn", "Users"));

        SearchResponse response = mock(SearchResponse.class);
        when(response.getEntries()).thenReturn(List.of(groupOne, groupTwo));

        try (MockedConstruction<SearchOperation> ignored = mockConstruction(SearchOperation.class,
                (mockOp, context) -> when(mockOp.execute(any(SearchRequest.class))).thenReturn(response))) {

            var resolver = new MemberGroupResolver("baseDN", mock(ConnectionFactory.class),
                    "(&(objectClass=inetOrgPerson)(member={0}))");

            Set<String> groups = resolver.resolveGroups(principal, entry);
            assertThat(groups, containsInAnyOrder("Admins", "Users"));
        }
    }

    @Test
    public void resolveGroups_ldapError_wrapsAsRuntimeException() {
        try (MockedConstruction<SearchOperation> ignored = mockConstruction(SearchOperation.class,
                (mockOp, context) -> when(mockOp.execute(any(SearchRequest.class))).thenThrow(new LdapException("boom")))) {

            var resolver = new MemberGroupResolver("baseDN", mock(ConnectionFactory.class),
                    "(&(objectClass=inetOrgPerson)(member={0}))");

            RuntimeException e = assertThrows(RuntimeException.class, () -> resolver.resolveGroups(principal, entry));
            assertThat(e.getMessage(), org.hamcrest.Matchers.startsWith("Failed executing LDAP query"));
            assertEquals("boom", e.getCause().getMessage());
        }
    }
}
