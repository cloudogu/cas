package de.triology.cas.ldap.resolvers;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CombinedGroupResolverTest {

    @Mock
    private GroupResolver resolverOne;

    @Mock
    private GroupResolver resolverTwo;

    @Mock
    private GroupResolver resolverThree;

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry entry;

    @Test
    public void resolveGroups() {
        when(resolverOne.resolveGroups(principal, entry)).thenReturn(new HashSet<>(Arrays.asList("a", "b")));
        when(resolverTwo.resolveGroups(principal, entry)).thenReturn(new HashSet<>(Arrays.asList("c", "d", "b")));
        CombinedGroupResolver resolver = new CombinedGroupResolver(Arrays.asList(resolverOne, resolverTwo, resolverThree));
        assertThat(resolver.resolveGroups(principal, entry), containsInAnyOrder("a", "b", "c", "d"));
    }

}