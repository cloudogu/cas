package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.CesLdapException;
import de.triology.cas.ldap.UserManager;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.oidc.profile.OidcProfileDefinition;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CesDelegatedClientUserProfileProvisionerTest {

    @Test
    public void testExecute_withExistingUser() throws Throwable {
        OidcProfile profile = new OidcProfile();
        profile.addAttribute(OidcProfileDefinition.PREFERRED_USERNAME, "test_user");
        profile.addAttribute(OidcProfileDefinition.GIVEN_NAME, "Test");
        profile.addAttribute(OidcProfileDefinition.FAMILY_NAME, "User");
        profile.addAttribute(OidcProfileDefinition.NAME, "Test User");
        profile.addAttribute(OidcProfileDefinition.EMAIL, "test@user.de");

        UserManager userManagerMock = mock(UserManager.class);
        CesInternalLdapUser testUser = new CesInternalLdapUser("test_user", "Test_old", "User_old", "Test User Old", "test_old@user.de", true);
        when(userManagerMock.getUserByUid("test_user")).thenReturn(testUser);

        CesInternalLdapUser expectedCesUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        Mockito.doNothing().when(userManagerMock).updateUser(expectedCesUser);

        CesDelegatedClientUserProfileProvisioner provisioner = new CesDelegatedClientUserProfileProvisioner(userManagerMock);
        provisioner.execute(null, profile, null, null);

        verify(userManagerMock, times(1)).updateUser(expectedCesUser);
    }

    @Test
    public void testExecute_withNewUser() throws Throwable {
        OidcProfile profile = new OidcProfile();
        profile.addAttribute(OidcProfileDefinition.PREFERRED_USERNAME, "test_user");
        profile.addAttribute(OidcProfileDefinition.GIVEN_NAME, "Test");
        profile.addAttribute(OidcProfileDefinition.FAMILY_NAME, "User");
        profile.addAttribute(OidcProfileDefinition.NAME, "Test User");
        profile.addAttribute(OidcProfileDefinition.EMAIL, "test@user.de");

        UserManager userManagerMock = mock(UserManager.class);
        when(userManagerMock.getUserByUid("test_user")).thenReturn(null);

        CesInternalLdapUser expectedCesUser = new CesInternalLdapUser("test_user", "Test", "User", "Test User", "test@user.de", true);
        Mockito.doNothing().when(userManagerMock).createUser(expectedCesUser);

        CesDelegatedClientUserProfileProvisioner provisioner = new CesDelegatedClientUserProfileProvisioner(userManagerMock);
        provisioner.execute(null, profile, null, null);

        verify(userManagerMock, times(1)).createUser(expectedCesUser);
    }

    @Test
    public void testExecute_withUnsupportedUserProfile() throws Throwable {
        CommonProfile profile = new CommonProfile();

        UserManager userManagerMock = mock(UserManager.class);

        CesDelegatedClientUserProfileProvisioner provisioner = new CesDelegatedClientUserProfileProvisioner(userManagerMock);

        RuntimeException e = assertThrows(RuntimeException.class, () -> provisioner.execute(null, profile, null, null));
        assertEquals("Unsupported profile type: CommonProfile", e.getMessage());
    }
}