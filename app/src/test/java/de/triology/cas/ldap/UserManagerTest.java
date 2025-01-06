package de.triology.cas.ldap;

import org.junit.Test;
import org.ldaptive.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserManagerTest {

    @Test
    public void testGetUserByUid() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        LdapEntry ldapEntry = new LdapEntry();
        ldapEntry.addAttributes(new LdapAttribute("uid", "test"));
        ldapEntry.addAttributes(new LdapAttribute("cn", "test"));
        ldapEntry.addAttributes(new LdapAttribute("sn", "User"));
        ldapEntry.addAttributes(new LdapAttribute("givenname", "Test"));
        ldapEntry.addAttributes(new LdapAttribute("displayName", "Test User"));
        ldapEntry.addAttributes(new LdapAttribute("mail", "test@user.de"));
        ldapEntry.addAttributes(new LdapAttribute("external", "TRUE"));
        ldapEntry.addAttributes(new LdapAttribute("memberOf", "group1", "group2"));

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(ldapEntry));
        when(responseMock.getEntry()).thenReturn(ldapEntry);

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesInternalLdapUser user = userManager.getUserByUid("test");

        assertNotNull(user);
        assertEquals("test", user.getUid());
        assertEquals("Test User", user.getDisplayName());
        assertEquals("User", user.getFamilyName());
        assertEquals("Test", user.getGivenName());
        assertEquals("test@user.de", user.getMail());
        assertArrayEquals(List.of("group2", "group1").toArray(), user.getGroups().toArray());
        assertTrue(user.isExternal());
    }

    @Test
    public void testGetUserByUid_notFound() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of());
        when(responseMock.getEntry()).thenReturn(null);

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesInternalLdapUser user = userManager.getUserByUid("test");

        assertNull(user);
    }

    @Test
    public void testGetUserByUid_moreResultsFound() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(new LdapEntry(), new LdapEntry()));

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByUid("test"));

        assertEquals("did not expect more then one result", e.getMessage());
    }

    @Test
    public void testGetUserByUid_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(false);
        when(responseMock.getDiagnosticMessage()).thenReturn("test error msg");

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByUid("test"));

        assertEquals("test error msg", e.getMessage());
    }

    @Test
    public void testGetUserByUid_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        when(searchOpMock.execute(any(SearchRequest.class))).thenThrow(new LdapException("test error"));

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByUid("test"));

        assertEquals("Failed executing LDAP query", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }

    @Test
    public void testCreateUser() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        AddOperation addOperation = mock(AddOperation.class);
        when(ofMock.addOperation()).thenReturn(addOperation);


        when(addOperation.execute(any(AddRequest.class))).then(invocation -> {
            AddRequest addRequest = invocation.getArgument(0, AddRequest.class);
            assertEquals("uid=test_user,baseDN", addRequest.getDn());
            assertEquals("objectClass", addRequest.getAttributes()[0].getName());
            assertArrayEquals(CesInternalLdapUser.ObjectClasses, addRequest.getAttributes()[0].getStringValues().toArray());
            assertEquals("cn", addRequest.getAttributes()[1].getName());
            assertEquals("test_user", addRequest.getAttributes()[1].getStringValue());
            assertEquals("sn", addRequest.getAttributes()[2].getName());
            assertEquals("User", addRequest.getAttributes()[2].getStringValue());
            assertEquals("givenname", addRequest.getAttributes()[3].getName());
            assertEquals("Test", addRequest.getAttributes()[3].getStringValue());
            assertEquals("displayName", addRequest.getAttributes()[4].getName());
            assertEquals("Test User", addRequest.getAttributes()[4].getStringValue());
            assertEquals("mail", addRequest.getAttributes()[5].getName());
            assertEquals("test@user.de", addRequest.getAttributes()[5].getStringValue());
            assertEquals("external", addRequest.getAttributes()[6].getName());
            assertEquals("TRUE", addRequest.getAttributes()[6].getStringValue());

            AddResponse responseMock = mock(AddResponse.class);
            when(responseMock.isSuccess()).thenReturn(true);
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        userManager.createUser(testUser);
    }

    @Test
    public void testCreateUser_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        AddOperation addOperation = mock(AddOperation.class);
        when(ofMock.addOperation()).thenReturn(addOperation);

        when(addOperation.execute(any(AddRequest.class))).then(invocation -> {
            AddResponse responseMock = mock(AddResponse.class);
            when(responseMock.isSuccess()).thenReturn(false);
            when(responseMock.getDiagnosticMessage()).thenReturn("test error create");
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);

        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.createUser(testUser));

        assertEquals("test error create", e.getMessage());
    }

    @Test
    public void testCreateUser_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        AddOperation addOperation = mock(AddOperation.class);
        when(ofMock.addOperation()).thenReturn(addOperation);

        when(addOperation.execute(any(AddRequest.class))).thenThrow(new LdapException("test error"));

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);

        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.createUser(testUser));

        assertEquals("error while creating user", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }

    @Test
    public void testUpdateUser() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).then(invocation -> {
            ModifyRequest modRequest = invocation.getArgument(0, ModifyRequest.class);
            assertEquals("uid=test_user,baseDN", modRequest.getDn());

            assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[0].getOperation());
            assertEquals("cn", modRequest.getModifications()[0].getAttribute().getName());
            assertEquals("test_user", modRequest.getModifications()[0].getAttribute().getStringValue());

            assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[1].getOperation());
            assertEquals("sn", modRequest.getModifications()[1].getAttribute().getName());
            assertEquals("User", modRequest.getModifications()[1].getAttribute().getStringValue());

            assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[2].getOperation());
            assertEquals("givenname", modRequest.getModifications()[2].getAttribute().getName());
            assertEquals("Test", modRequest.getModifications()[2].getAttribute().getStringValue());

            assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[3].getOperation());
            assertEquals("displayName", modRequest.getModifications()[3].getAttribute().getName());
            assertEquals("Test User", modRequest.getModifications()[3].getAttribute().getStringValue());

            assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[4].getOperation());
            assertEquals("mail", modRequest.getModifications()[4].getAttribute().getName());
            assertEquals("test@user.de", modRequest.getModifications()[4].getAttribute().getStringValue());

            ModifyResponse responseMock = mock(ModifyResponse.class);
            when(responseMock.isSuccess()).thenReturn(true);
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        userManager.updateUser(testUser);
    }

    @Test
    public void testUpdateUser_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).then(invocation -> {
            ModifyResponse responseMock = mock(ModifyResponse.class);
            when(responseMock.isSuccess()).thenReturn(false);
            when(responseMock.getDiagnosticMessage()).thenReturn("test error create");
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);

        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.updateUser(testUser));

        assertEquals("test error create", e.getMessage());
    }

    @Test
    public void testUpdateUser_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).thenThrow(new LdapException("test error"));

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);

        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.updateUser(testUser));

        assertEquals("error while updating user", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }

    @Test
    public void testAddUserToGroup() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).then(invocation -> {
            ModifyRequest modRequest = invocation.getArgument(0, ModifyRequest.class);
            assertEquals("cn=group1,ou=Groups,o=test,dc=cloudogu,dc=com", modRequest.getDn());

            assertEquals(AttributeModification.Type.ADD, modRequest.getModifications()[0].getOperation());
            assertEquals("member", modRequest.getModifications()[0].getAttribute().getName());
            assertEquals("uid=test_user,ou=People,o=test,dc=cloudogu,dc=com", modRequest.getModifications()[0].getAttribute().getStringValue());

            ModifyResponse responseMock = mock(ModifyResponse.class);
            when(responseMock.isSuccess()).thenReturn(true);
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("ou=People,o=test,dc=cloudogu,dc=com", ofMock);
        userManager.addUserToGroup(testUser, "group1");
    }

    @Test
    public void testAddUserToGroup_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).then(invocation -> {
            ModifyRequest modRequest = invocation.getArgument(0, ModifyRequest.class);
            assertEquals("cn=group1,baseDN", modRequest.getDn());

            assertEquals(AttributeModification.Type.ADD, modRequest.getModifications()[0].getOperation());
            assertEquals("member", modRequest.getModifications()[0].getAttribute().getName());
            assertEquals("uid=test_user,baseDN", modRequest.getModifications()[0].getAttribute().getStringValue());

            ModifyResponse responseMock = mock(ModifyResponse.class);
            when(responseMock.isSuccess()).thenReturn(false);
            when(responseMock.getDiagnosticMessage()).thenReturn("test error create");
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.addUserToGroup(testUser, "group1"));

        assertEquals("test error create", e.getMessage());
    }

    @Test
    public void testAddUserToGroup_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).thenThrow(new LdapException("test error"));

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.addUserToGroup(testUser, "group1"));

        assertEquals("error while adding user to group", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }
}