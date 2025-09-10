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
        final CesInternalLdapUser fromProfile = userFromProfile(profile);

        // 1) Try by uid (external users)
        CesInternalLdapUser byUid = userManager.getUserByUid(fromProfile.getUid());
        if (byUid != null) {
            userManager.updateUser(mergeForUpdate(byUid.getUid(), fromProfile));
            return;
        }

        // 2) Try by mail (any user; only fetch uid to avoid NPE on missing attributes)
        String existingUid = userManager.getUidByMail(fromProfile.getMail());
        if (existingUid != null) {
            userManager.updateUser(mergeForUpdate(existingUid, fromProfile)); // will set external=TRUE
            return;
        }

        // 3) Create new
        userManager.createUser(fromProfile);
        addAdminGroupsForUser(fromProfile, principal);
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
    
    private static CesInternalLdapUser mergeForUpdate(String existingUid, CesInternalLdapUser fromProfile) {
        return new CesInternalLdapUser(
            existingUid,
            fromProfile.getGivenName(),
            fromProfile.getFamilyName(),
            fromProfile.getDisplayName(),
            fromProfile.getMail(),
            true // mark as external when OIDC-provisioned
        );
    }
}
