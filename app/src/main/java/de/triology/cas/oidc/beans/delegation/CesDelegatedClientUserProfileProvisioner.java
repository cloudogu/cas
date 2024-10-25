package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesLdapUser;
import de.triology.cas.ldap.UserManager;
import de.triology.cas.ldap.resolvers.GroupResolver;
import lombok.RequiredArgsConstructor;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.ldaptive.LdapEntry;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.profile.UserProfile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CesDelegatedClientUserProfileProvisioner implements DelegatedClientUserProfileProvisioner {

    private final UserManager userManager;

    @Override
    public void execute(Principal principal, UserProfile profile, BaseClient client, Credential credential) throws Throwable {
        System.out.println("############ here we are");

        CesLdapUser userFromProfile = CesLdapUser.UserFromProfile(profile);

        CesLdapUser existingLdapUser = userManager.getUserByUid(userFromProfile.getUid());
        if (existingLdapUser == null) {
            // user does not exist -> create
            userManager.createUser(userFromProfile);
        } else {
            //merge
            userFromProfile.setGroups(existingLdapUser.getGroups());

            // user does not exist -> update
            userManager.updateUser(userFromProfile);
        }

        //update principal
        Map<String, List<Object>> principalAttributes = principal.getAttributes();
        //Fixme mapping
        principalAttributes.put("cn", List.of(userFromProfile.getUid()));
        principalAttributes.put("username", List.of(userFromProfile.getUid()));
        principalAttributes.put("displayName", List.of(userFromProfile.getDisplayName()));
        principalAttributes.put("surname", List.of(userFromProfile.getFamilyName()));
        principalAttributes.put("givenName", List.of(userFromProfile.getGivenName()));
        principalAttributes.put("mail", List.of(userFromProfile.getMail()));
        principalAttributes.put("external", List.of(Boolean.toString(userFromProfile.isExternal())));

        principal.getAttributes().put("groups", Arrays.asList(userFromProfile.getGroups().toArray()));
    }
}
