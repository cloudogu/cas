package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.CesLdapException;
import de.triology.cas.ldap.UserManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;

import java.util.List;

/**
 * The CesDelegatedClientUserProfileProvisioner handles th provisioning of UserProfiles from the delegated authentication.
 * Currently only supports OIDC-UserProfiles ({@link OidcProfile}) and the internal CES-LDAP.
 * The CesDelegatedClientUserProfileProvisioner checks if a user for the given userProfile exists in the internal CES-LDAP.
 * If no user exists a new user will be created. If the user already exists, it will be updated with the values from the userProfile.
 *
 * The CesDelegatedClientUserProfileProvisioner also checks if the user belongs to the initial admin-users and assigns the admin-groups accordingly.
 */
@RequiredArgsConstructor
public class CesDelegatedClientUserProfileProvisioner implements DelegatedClientUserProfileProvisioner {

    private final UserManager userManager;

    private final String[] initialAdminUsernames;

    private final String[] adminGroups;

    @Override
    public void execute(Principal principal, UserProfile profile, BaseClient client, Credential credential) throws Throwable {
        CesInternalLdapUser userFromProfile = userFromProfile(profile);

        CesInternalLdapUser existingLdapUser = userManager.getUserByUid(userFromProfile.getUid());
        if (existingLdapUser == null) {
            // user does not exist -> create
            userManager.createUser(userFromProfile);

            addAdminGroupsForUser(userFromProfile, principal);
        } else {
            // user does not exist -> update
            userManager.updateUser(userFromProfile);
        }
    }

    private void addAdminGroupsForUser(CesInternalLdapUser user, Principal principal) throws CesLdapException {
        List<Object> principalGroups = PrincipalGroups.getGroupsFromPrincipal(principal);

        if (ArrayUtils.contains(initialAdminUsernames, user.getUid())) {
            for (final String group : adminGroups) {
                userManager.addUserToGroup(user, group);
                principalGroups.add(group);
            }

            PrincipalGroups.setGroupsInPrincipal(principal, principalGroups);
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
