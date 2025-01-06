package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.UserManager;
import org.apereo.cas.authentication.adaptive.UnauthorizedAuthenticationException;
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

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, null);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(7, resultPrincipal.getAttributes().size());
        assertEquals(true, resultPrincipal.getAttributes().get("cesAttributesAlreadyMapped").getFirst());
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

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, null);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(6, resultPrincipal.getAttributes().size());
        assertEquals(true, resultPrincipal.getAttributes().get("cesAttributesAlreadyMapped").getFirst());
        assertEquals("Test", resultPrincipal.getAttributes().get("given_name").getFirst());
        assertEquals("Test", resultPrincipal.getAttributes().get("firstName").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("family_name").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("lastName").getFirst());
        assertEquals("test_user", resultPrincipal.getAttributes().get("username").getFirst());
        assertNull(resultPrincipal.getAttributes().get("groups"));
    }

    @Test
    public void testProcess_alreadyMapped() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        attributes.put("cesAttributesAlreadyMapped", List.of(true));
        when(principal.getAttributes()).thenReturn(attributes);

        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, null);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(5, resultPrincipal.getAttributes().size());
        assertEquals(true, resultPrincipal.getAttributes().get("cesAttributesAlreadyMapped").getFirst());
        assertEquals("Test", resultPrincipal.getAttributes().get("given_name").getFirst());
        assertEquals("User", resultPrincipal.getAttributes().get("family_name").getFirst());
        assertEquals("test_user", resultPrincipal.getAttributes().get("username").getFirst());
        assertArrayEquals(testUser.getGroups().toArray(), resultPrincipal.getAttributes().get("groups").toArray());
    }

    @Test
    public void testProcessCheckAllowedGroups() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        attributes.put("groups", List.of("group1", "group3"));
        when(principal.getAttributes()).thenReturn(attributes);

        String[] allowedGroups = {"group1", "group2"};

        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName"),
                new AttributeMapping("groups", "externalGroups")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, allowedGroups);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(8, resultPrincipal.getAttributes().size());
        assertEquals(true, resultPrincipal.getAttributes().get("cesAttributesAlreadyMapped").getFirst());
        assertEquals(List.of("group1", "group3"), resultPrincipal.getAttributes().get("externalGroups"));
        assertArrayEquals(testUser.getGroups().toArray(), resultPrincipal.getAttributes().get("groups").toArray());
    }

    @Test
    public void testProcessCheckAllowedGroups_AllAllowed() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        attributes.put("groups", List.of("group44"));
        when(principal.getAttributes()).thenReturn(attributes);

        String[] allowedGroups = {};

        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName"),
                new AttributeMapping("groups", "externalGroups")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, allowedGroups);
        Principal resultPrincipal = preProcessor.process(principal, null, null, null);

        assertNotNull(resultPrincipal);
        assertEquals(8, resultPrincipal.getAttributes().size());
    }

    @Test
    public void testProcessCheckAllowedGroups_NotAllowed() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        attributes.put("groups", List.of("group1", "group3"));
        when(principal.getAttributes()).thenReturn(attributes);

        String[] allowedGroups = {"group2"};

        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName"),
                new AttributeMapping("groups", "externalGroups")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, allowedGroups);

        UnauthorizedAuthenticationException exception = assertThrows(UnauthorizedAuthenticationException.class, () -> preProcessor.process(principal, null, null, null));

        assertEquals("user is not assigned to any of the allowed groups", exception.getMessage());
    }

    @Test
    public void testProcessCheckAllowedGroups_NotAllowed_NoGroups() throws Throwable {
        Principal principal = mock(Principal.class);
        when(principal.getId()).thenReturn("test_user");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("given_name", List.of("Test"));
        attributes.put("family_name", List.of("User"));
        attributes.put("username", List.of("test_user"));
        when(principal.getAttributes()).thenReturn(attributes);

        String[] allowedGroups = {"group2"};

        List<AttributeMapping> mappings = List.of(
                new AttributeMapping("given_name", "firstName"),
                new AttributeMapping("family_name", "lastName"),
                new AttributeMapping("groups", "externalGroups")
        );
        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        testUser.setGroups(Set.of("group1", "group2"));
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesDelegatedAuthenticationPreProcessor preProcessor = new CesDelegatedAuthenticationPreProcessor(userManagerMock, mappings, allowedGroups);

        UnauthorizedAuthenticationException exception = assertThrows(UnauthorizedAuthenticationException.class, () -> preProcessor.process(principal, null, null, null));

        assertEquals("user is not assigned to any of the allowed groups", exception.getMessage());
    }
}