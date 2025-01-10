package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.UserManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.adaptive.UnauthorizedAuthenticationException;
import org.apereo.cas.authentication.principal.DelegatedAuthenticationPreProcessor;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;
import org.pac4j.core.client.BaseClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The CesDelegatedAuthenticationPreProcessor handles attribute-mapping for the delegated authentication.
 * First all attributes in the give principal are mapped according to the configured attributeMappings.
 * Then the user for the principal will be loaded form the internal CES-LDAP using the ID of the principal.
 * If the user exists, the groups of the user will be added as an attribute the principal.
 */
@RequiredArgsConstructor
public class CesDelegatedAuthenticationPreProcessor implements DelegatedAuthenticationPreProcessor {

    private final static String alreadyMappedAttributeName = "cesAttributesAlreadyMapped";
    private final static String externalGroupsAttributeName = "externalGroups";

    private final UserManager userManager;

    private final List<AttributeMapping> attributeMappings;

    private final String[] allowedGroups;

    @Override
    public Principal process(Principal principal, BaseClient client, Credential credential, Service service) throws Throwable {
        mapAttributes(principal);

        if (!checkExternalAllowedGroups(principal)) {
            throw new UnauthorizedAuthenticationException("user is not assigned to any of the allowed groups");
        }

        // attach internal groups
        CesInternalLdapUser existingLdapUser = userManager.getUserByUid(principal.getId());
        if (existingLdapUser != null) {
            PrincipalGroups.setGroupsInPrincipal(principal, Arrays.asList(existingLdapUser.getGroups().toArray()));
        }

        return principal;
    }

    private void mapAttributes(Principal principal) {
        Map<String, List<Object>> principalAttributes = principal.getAttributes();

        if (principalAttributes.containsKey(alreadyMappedAttributeName)) {
            // attributes were already mapped
            return;
        }

        for (AttributeMapping mapping : attributeMappings) {
            List<Object> attributeValues = principalAttributes.get(mapping.getSource());
            if (attributeValues != null) {
                principalAttributes.put(mapping.getTarget(), attributeValues);
            }
        }

        principalAttributes.put(alreadyMappedAttributeName, List.of(true));
    }

    private boolean checkExternalAllowedGroups(Principal principal) {
        if (ArrayUtils.isEmpty(allowedGroups)) {
            // no allowed groups configured -> access for all
            return true;
        }

        List<Object> groups = principal.getAttributes().get(externalGroupsAttributeName);
        if (groups == null) {
            return false;
        }

        for (Object group : groups) {
            if (ArrayUtils.contains(allowedGroups, group.toString())) {
                return true;
            }
        }

        return false;

    }
}
