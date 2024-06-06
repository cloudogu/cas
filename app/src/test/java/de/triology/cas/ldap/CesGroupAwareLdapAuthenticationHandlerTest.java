package de.triology.cas.ldap;

import de.triology.cas.ldap.resolvers.GroupResolver;
import org.apereo.cas.authentication.principal.Principal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CesGroupAwareLdapAuthenticationHandlerTest {

    @Mock
    private Authenticator authenticator;

    @Mock
    private GroupResolver resolver;

    @InjectMocks
    private CesGroupAwareLdapAuthenticationHandler handler;

    @Mock
    private Principal principal;

    @Mock
    private LdapEntry ldapEntry;

    @Test
    public void attachGroups() throws Throwable {
        Map<String, List<Object>> attributes = new HashMap<>();
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
