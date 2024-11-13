package de.triology.cas.oidc.beans.delegation;

import de.triology.cas.ldap.CesInternalLdapUser;
import de.triology.cas.ldap.UserManager;
import lombok.RequiredArgsConstructor;
import org.apereo.cas.authentication.Credential;
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

    private final static String groupsAttributeName = "groups";

    private final List<AttributeMapping> attributeMappings;

    private final UserManager userManager;

    @Override
    public Principal process(Principal principal, BaseClient client, Credential credential, Service service) throws Throwable {

        // map attributes
        Map<String, List<Object>> principalAttributes = principal.getAttributes();
        for (AttributeMapping mapping : attributeMappings) {
            List<Object> attributeValues = principalAttributes.get(mapping.getSource());
            if (attributeValues != null) {
                principalAttributes.put(mapping.getTarget(), attributeValues);
            }
        }

        // attach groups
        CesInternalLdapUser existingLdapUser = userManager.getUserByUid(principal.getId());
        if (existingLdapUser != null) {
            principal.getAttributes().put(groupsAttributeName, Arrays.asList(existingLdapUser.getGroups().toArray()));
        }

        return principal;
    }
}
