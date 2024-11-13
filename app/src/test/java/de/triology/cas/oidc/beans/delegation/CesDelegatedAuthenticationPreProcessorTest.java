package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.UserManager;
import org.apereo.cas.authentication.principal.Principal;
import org.junit.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CesDelegatedAuthenticationPreProcessorTest {

    @Test
    public void testProcess() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        when(principal.getAttributes()).thenReturn(attributes);


        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(mappings, userManagerMock);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(6, resultPrincipal.getAttributes().size());
        assertEquals("Test", resultPrincipal.getAttributes().get("given_name").getFirst());
        assertEquals("Test", resultPrincipal.getAttributes().get("firstName").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("family_name").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("lastName").getFirst());
        assertEquals("test_user", resultPrincipal.getAttributes().get("username").getFirst());
        assertArrayEquals(testUser.getGroups().toArray(), resultPrincipal.getAttributes().get("groups").toArray());
    }

    @Test
    public void testProcess_withNonExistingLdapUser() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        when(principal.getAttributes()).thenReturn(attributes);


        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName"),
                new AttributeMapping("foo", "bar")
        );
        UserManager userManagerMock = mock(UserManager.class);
        when(userManagerMock.getUserByUid("test_user")).thenReturn(null);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(mappings, userManagerMock);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(5, resultPrincipal.getAttributes().size());
        assertEquals("Test", resultPrincipal.getAttributes().get("given_name").getFirst());
        assertEquals("Test", resultPrincipal.getAttributes().get("firstName").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("family_name").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("lastName").getFirst());
        assertEquals("test_user", resultPrincipal.getAttributes().get("username").getFirst());
        assertNull(resultPrincipal.getAttributes().get("groups"));
    }
}