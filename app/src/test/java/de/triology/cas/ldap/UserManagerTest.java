package de.triology.cas.ldap;

import org.junit.jupiter.api.Test;
import org.ldaptive.*;
import org.ldaptive.filter.AndFilter;
import org.ldaptive.filter.EqualityFilter;

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

    // updateUser(): when the user is external, updateUser() first calls the private
    // ensureAuxObjectClass() helper, which issues its OWN modifyOperation().execute() call
    // (a single ADD-objectClass modification) before the "real" 6-attribute REPLACE update.
    // The tests below stub the two calls distinctly (via consecutive thenAnswer()) instead of
    // a single catch-all answer, since the two ModifyRequests have different shapes.

    @Test
    public void testUpdateUser() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class)))
                .thenAnswer(invocation -> {
                    // 1st call: ensureAuxObjectClass() adding the 'cesperson' aux objectClass
                    ModifyRequest modRequest = invocation.getArgument(0, ModifyRequest.class);
                    assertEquals("uid=test_user,baseDN", modRequest.getDn());
                    assertEquals(1, modRequest.getModifications().length);
                    assertEquals(AttributeModification.Type.ADD, modRequest.getModifications()[0].getOperation());
                    assertEquals("objectClass", modRequest.getModifications()[0].getAttribute().getName());
                    assertEquals("cesperson", modRequest.getModifications()[0].getAttribute().getStringValue());

                    ModifyResponse responseMock = mock(ModifyResponse.class);
                    when(responseMock.isSuccess()).thenReturn(true);
                    return responseMock;
                })
                .thenAnswer(invocation -> {
                    // 2nd call: the actual attribute update
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

                    assertEquals(AttributeModification.Type.REPLACE, modRequest.getModifications()[5].getOperation());
                    assertEquals("external", modRequest.getModifications()[5].getAttribute().getName());
                    assertEquals("TRUE", modRequest.getModifications()[5].getAttribute().getStringValue());

                    ModifyResponse responseMock = mock(ModifyResponse.class);
                    when(responseMock.isSuccess()).thenReturn(true);
                    return responseMock;
                });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        userManager.updateUser(testUser);

        verify(modifyOperation, times(2)).execute(any(ModifyRequest.class));
    }

    @Test
    public void testUpdateUser_notExternal_skipsEnsureAuxObjectClass() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).thenAnswer(invocation -> {
            ModifyRequest modRequest = invocation.getArgument(0, ModifyRequest.class);
            assertEquals(6, modRequest.getModifications().length);
            assertEquals("FALSE", modRequest.getModifications()[5].getAttribute().getStringValue());

            ModifyResponse responseMock = mock(ModifyResponse.class);
            when(responseMock.isSuccess()).thenReturn(true);
            return responseMock;
        });

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", false);

        UserManager userManager = new UserManager("baseDN", ofMock);
        userManager.updateUser(testUser);

        // ensureAuxObjectClass() must NOT run for non-external users: exactly one modify call.
        verify(modifyOperation, times(1)).execute(any(ModifyRequest.class));
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

        // external=false so the failure is isolated to the "real" update call, not ensureAuxObjectClass().
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", false);

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

        // external=false so the failure is isolated to the "real" update call, not ensureAuxObjectClass().
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", false);

        UserManager userManager = new UserManager("baseDN", ofMock);

        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.updateUser(testUser));

        assertEquals("error while updating user", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }

    @Test
    public void testUpdateUser_ensureAuxObjectClass_toleratesAttributeOrValueExists() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        ModifyResponse ensureAuxResponse = mock(ModifyResponse.class);
        when(ensureAuxResponse.isSuccess()).thenReturn(false);
        when(ensureAuxResponse.getResultCode()).thenReturn(ResultCode.ATTRIBUTE_OR_VALUE_EXISTS);

        ModifyResponse updateResponse = mock(ModifyResponse.class);
        when(updateResponse.isSuccess()).thenReturn(true);

        when(modifyOperation.execute(any(ModifyRequest.class)))
                .thenReturn(ensureAuxResponse)
                .thenReturn(updateResponse);

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        // Must not throw: ATTRIBUTE_OR_VALUE_EXISTS on the aux-objectClass add is tolerated.
        userManager.updateUser(testUser);

        verify(modifyOperation, times(2)).execute(any(ModifyRequest.class));
    }

    @Test
    public void testUpdateUser_ensureAuxObjectClass_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        ModifyResponse ensureAuxResponse = mock(ModifyResponse.class);
        when(ensureAuxResponse.isSuccess()).thenReturn(false);
        when(ensureAuxResponse.getResultCode()).thenReturn(ResultCode.INSUFFICIENT_ACCESS_RIGHTS);
        when(ensureAuxResponse.getDiagnosticMessage()).thenReturn("no rights to add objectClass");

        when(modifyOperation.execute(any(ModifyRequest.class))).thenReturn(ensureAuxResponse);

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.updateUser(testUser));

        assertEquals("no rights to add objectClass", e.getMessage());
        // The "real" update must never run once ensureAuxObjectClass() fails.
        verify(modifyOperation, times(1)).execute(any(ModifyRequest.class));
    }

    @Test
    public void testUpdateUser_ensureAuxObjectClass_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        ModifyOperation modifyOperation = mock(ModifyOperation.class);
        when(ofMock.modifyOperation()).thenReturn(modifyOperation);

        when(modifyOperation.execute(any(ModifyRequest.class))).thenThrow(new LdapException("aux error"));

        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.updateUser(testUser));

        assertEquals("failed to ensure objectClass cesperson for uid=test_user,baseDN", e.getMessage());
        assertEquals("aux error", e.getCause().getMessage());
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

    @Test
    public void testGetUserByMail() throws Throwable {
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
        ldapEntry.addAttributes(new LdapAttribute("external", "FALSE"));

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(ldapEntry));
        when(responseMock.getEntry()).thenReturn(ldapEntry);

        when(searchOpMock.execute(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0, SearchRequest.class);
            // default overload (externalOnly=false) must search directory-wide, not just external users.
            assertEquals(new EqualityFilter("mail", "test@user.de"), request.getFilter());
            return responseMock;
        });

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesInternalLdapUser user = userManager.getUserByMail("test@user.de");

        assertNotNull(user);
        assertEquals("test", user.getUid());
        assertFalse(user.isExternal());
    }

    @Test
    public void testGetUserByMail_externalOnly() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of());
        when(responseMock.getEntry()).thenReturn(null);

        when(searchOpMock.execute(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0, SearchRequest.class);
            assertEquals(
                    new AndFilter(new EqualityFilter("mail", "test@user.de"), new EqualityFilter("external", "TRUE")),
                    request.getFilter());
            return responseMock;
        });

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesInternalLdapUser user = userManager.getUserByMail("test@user.de", true);

        assertNull(user);
    }

    @Test
    public void testGetUserByMail_moreResultsFound() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(new LdapEntry(), new LdapEntry()));

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByMail("test@user.de"));

        assertEquals("did not expect more then one result for mail=test@user.de", e.getMessage());
    }

    @Test
    public void testGetUserByMail_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(false);
        when(responseMock.getDiagnosticMessage()).thenReturn("test error msg");

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByMail("test@user.de"));

        assertEquals("test error msg", e.getMessage());
    }

    @Test
    public void testGetUserByMail_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        when(searchOpMock.execute(any(SearchRequest.class))).thenThrow(new LdapException("test error"));

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUserByMail("test@user.de"));

        assertEquals("Failed executing LDAP query", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }

    @Test
    public void testGetUidByMail() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        LdapEntry ldapEntry = new LdapEntry();
        ldapEntry.addAttributes(new LdapAttribute("uid", "test"));

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(ldapEntry));
        when(responseMock.getEntry()).thenReturn(ldapEntry);

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        String uid = userManager.getUidByMail("test@user.de");

        assertEquals("test", uid);
    }

    @Test
    public void testGetUidByMail_notFound() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of());
        when(responseMock.getEntry()).thenReturn(null);

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        assertNull(userManager.getUidByMail("test@user.de"));
    }

    @Test
    public void testGetUidByMail_entryWithoutUidAttribute() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        LdapEntry ldapEntry = new LdapEntry(); // no "uid" attribute present

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(ldapEntry));
        when(responseMock.getEntry()).thenReturn(ldapEntry);

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        assertNull(userManager.getUidByMail("test@user.de"));
    }

    @Test
    public void testGetUidByMail_duplicateFound() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(true);
        when(responseMock.getEntries()).thenReturn(List.of(new LdapEntry(), new LdapEntry()));

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUidByMail("test@user.de"));

        assertEquals("did not expect more then one result for mail=test@user.de", e.getMessage());
    }

    @Test
    public void testGetUidByMail_notSuccessful() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        SearchResponse responseMock = mock(SearchResponse.class);
        when(responseMock.isSuccess()).thenReturn(false);
        when(responseMock.getDiagnosticMessage()).thenReturn("test error msg");

        when(searchOpMock.execute(any(SearchRequest.class))).thenReturn(responseMock);

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUidByMail("test@user.de"));

        assertEquals("test error msg", e.getMessage());
    }

    @Test
    public void testGetUidByMail_ldapError() throws Throwable {
        LdapOperationFactory ofMock = mock(LdapOperationFactory.class);
        SearchOperation searchOpMock = mock(SearchOperation.class);
        when(ofMock.searchOperation()).thenReturn(searchOpMock);

        when(searchOpMock.execute(any(SearchRequest.class))).thenThrow(new LdapException("test error"));

        UserManager userManager = new UserManager("baseDN", ofMock);
        CesLdapException e = assertThrows(CesLdapException.class, () -> userManager.getUidByMail("test@user.de"));

        assertEquals("Failed executing LDAP query", e.getMessage());
        assertEquals("test error", e.getCause().getMessage());
    }
}
