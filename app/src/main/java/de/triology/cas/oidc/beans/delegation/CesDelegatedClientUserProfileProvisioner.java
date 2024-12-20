package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.UserManager;
import lombok.RequiredArgsConstructor;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;

/**
 * The CesDelegatedClientUserProfileProvisioner handles th provisioning of UserProfiles from the delegated authentication.
 * Currently only supports OIDC-UserProfiles ({@link OidcProfile}) and the internal CES-LDAP.
 * The CesDelegatedClientUserProfileProvisioner checks if a user for the given userProfile exists in the internal CES-LDAP.
 * If no user exists a new user will be created. If the user already exists, it will be updated with the values from the userProfile.
 */
@RequiredArgsConstructor
public class CesDelegatedClientUserProfileProvisioner implements DelegatedClientUserProfileProvisioner {

    private final UserManager userManager;

    @Override
    public void execute(Principal principal, UserProfile profile, BaseClient client, Credential credential) throws Throwable {
        CesInternalLdapUser userFromProfile = userFromProfile(profile);

        CesInternalLdapUser existingLdapUser = userManager.getUserByUid(userFromProfile.getUid());
        if (existingLdapUser == null) {
            // user does not exist -> create
            userManager.createUser(userFromProfile);
        } else {
            // user does not exist -> update
            userManager.updateUser(userFromProfile);
        }
    }

    private static CesInternalLdapUser userFromProfile(UserProfile profile) {
        if (profile instanceof OidcProfile oidcProfile) {
            return new CesInternalLdapUser(
                    oidcProfile.getUsername(),
                    oidcProfile.getFirstName(),
                    oidcProfile.getFamilyName(),
                    oidcProfile.getDisplayName(),
                    oidcProfile.getEmail(),
                    true
            );
        }

        throw new RuntimeException("Unsupported profile type: " + profile.getClass().getSimpleName());
    }
}
