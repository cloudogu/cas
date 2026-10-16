package de.triology.cas.sync;

import de.triology.cas.ldap.CesLdapException;
import de.triology.cas.ldap.LdapUserReference;
import de.triology.cas.ldap.UserManager;
import de.triology.cas.pat.service.PATService;
import de.triology.cas.pat.service.PATOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class OidcLdapSynchronizerTest {
    private ScimClient scimClient;
    private UserManager userManager;
    private OidcLdapSyncProperties properties;
    private OidcLdapSynchronizer synchronizer;
    private PATService patService;

    @BeforeEach
    void setUp() {
        scimClient = mock(ScimClient.class);
        userManager = mock(UserManager.class);
        patService = mock(PATService.class);
        properties = new OidcLdapSyncProperties();
        properties.setTimeout(Duration.ofMinutes(5));
        properties.getScim().setGroupIds(List.of("group-a", "group-b"));
        synchronizer = new OidcLdapSynchronizer(scimClient, userManager, properties, patService);
    }

    @Test
    void deletesExternalUsersNotPresentInAnyConfiguredGroup() throws Exception {
        when(scimClient.getGroupMembers(eq("group-a"), anyLong()))
                .thenReturn(Set.of(new ScimClient.GroupMember("scim-keep", "keep")));
        when(scimClient.getGroupMembers(eq("group-b"), anyLong()))
                .thenReturn(Set.of(new ScimClient.GroupMember("scim-also-keep", "also-keep")));
        when(userManager.getExternalUsers(any(), any(), any(), anyLong()))
                .thenReturn(List.of(
                        new LdapUserReference("uid=keep,ou=People", "keep"),
                        new LdapUserReference("uid=remove,ou=People", "remove")));

        synchronizer.synchronize();

        verify(userManager).deleteUser("uid=remove,ou=People");
        verify(userManager, never()).deleteUser("uid=keep,ou=People");
        verify(patService).deleteAllForUser("remove");
        verifyNoMoreInteractions(patService);
    }

    @Test
    void emptySuccessfullyLoadedGroupsDeleteAllExternalUsers() throws Exception {
        properties.getScim().setGroupIds(List.of("empty-group"));
        when(scimClient.getGroupMembers(eq("empty-group"), anyLong())).thenReturn(Set.of());
        when(userManager.getExternalUsers(any(), any(), any(), anyLong()))
                .thenReturn(List.of(new LdapUserReference("uid=remove,ou=People", "remove")));

        synchronizer.synchronize();

        verify(userManager).deleteUser("uid=remove,ou=People");
        verify(patService).deleteAllForUser("remove");
    }

    @Test
    void abortsBeforeLdapWhenScimGroupCannotBeLoaded() throws Exception {
        when(scimClient.getGroupMembers(any(), anyLong()))
                .thenThrow(new IllegalStateException("SCIM unavailable"));

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize());

        verifyNoInteractions(userManager);
    }

    @Test
    void rejectsMissingGroupConfiguration() {
        properties.getScim().setGroupIds(List.of());

        assertThrows(IllegalStateException.class, () -> synchronizer.synchronize());

        verifyNoInteractions(scimClient, userManager);
    }

    @Test
    void rejectsNullAndBlankGroupIdsBeforeLdapDeletion() {
        for (String invalid : Arrays.asList(null, "", " ")) {
            properties.getScim().setGroupIds(Arrays.asList(invalid));
            assertThrows(IllegalStateException.class, synchronizer::synchronize);
        }
        verifyNoInteractions(scimClient, userManager, patService);
    }

    @Test
    void stopsBeforeLdapWhenLoadingGroupsExceedsDeadline() {
        properties.setTimeout(Duration.ZERO);
        assertThrows(IllegalStateException.class, synchronizer::synchronize);
        verifyNoInteractions(userManager, patService);
    }

    @Test
    void stopsBeforeDeletingUsersWhenLdapSearchExceedsDeadline() throws Exception {
        when(userManager.getExternalUsers(any(), any(), any(), anyLong())).thenAnswer(invocation -> {
            long deadline = invocation.getArgument(3);
            while (System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            return List.of(new LdapUserReference("uid=remove,ou=People", "remove"));
        });
        properties.setTimeout(Duration.ofMillis(20));

        assertThrows(IllegalStateException.class, synchronizer::synchronize);

        verify(userManager, never()).deleteUser(any());
        verifyNoInteractions(patService);
    }

    @Test
    void deletesUsersWithPatServiceDisabled() throws Exception {
        when(userManager.getExternalUsers(any(), any(), any(), anyLong()))
                .thenReturn(List.of(new LdapUserReference("uid=remove,ou=People", "remove")));
        var withoutPats = new OidcLdapSynchronizer(scimClient, userManager, properties, null);

        withoutPats.synchronize();

        verify(userManager).deleteUser("uid=remove,ou=People");
        verifyNoInteractions(patService);
    }

    @Test
    void continuesAfterPatOperationFailureForDeletedUser() throws Exception {
        when(userManager.getExternalUsers(any(), any(), any(), anyLong()))
                .thenReturn(List.of(new LdapUserReference("uid=first,ou=People", "first"),
                        new LdapUserReference("uid=second,ou=People", "second")));
        doThrow(new PATOperationException()).when(patService).deleteAllForUser("first");

        synchronizer.synchronize();

        verify(userManager).deleteUser("uid=first,ou=People");
        verify(userManager).deleteUser("uid=second,ou=People");
        verify(patService).deleteAllForUser("second");
    }

    @Test
    void stopsAndDoesNotRemovePatsWhenLdapDeletionFails() throws Exception {
        when(userManager.getExternalUsers(any(), any(), any(), anyLong()))
                .thenReturn(List.of(new LdapUserReference("uid=remove,ou=People", "remove")));
        doThrow(new CesLdapException("LDAP unavailable")).when(userManager).deleteUser(any());

        assertThrows(CesLdapException.class, synchronizer::synchronize);

        verifyNoInteractions(patService);
    }

    @Test
    void formatsAuditFieldsForDeletedUser() {
        String event = OidcLdapSynchronizer.deletionAuditMessage("remove", "uid=remove,ou=People",
                Instant.parse("2026-01-02T03:04:05Z"));

        org.junit.jupiter.api.Assertions.assertTrue(event.contains("scimId=not-present"));
        org.junit.jupiter.api.Assertions.assertTrue(event.contains("identity=remove"));
        org.junit.jupiter.api.Assertions.assertTrue(event.contains("dn=uid=remove,ou=People"));
        org.junit.jupiter.api.Assertions.assertTrue(event.contains("deletedAt=2026-01-02T03:04:05Z"));
        org.junit.jupiter.api.Assertions.assertTrue(event.contains("reason=identity is not present"));
    }
}
