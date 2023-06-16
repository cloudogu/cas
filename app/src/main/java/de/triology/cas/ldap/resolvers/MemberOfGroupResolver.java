package de.triology.cas.ldap.resolvers;

import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.ldaptive.LdapEntry;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolve groups by reading an attribute from the ldap entry. <strong>Note</strong> you
 * have to add the name of the attribute to the additionalAttributes list of
 * {@link CesGroupAwareLdapAuthenticationHandler}.
 */
@Slf4j
public class MemberOfGroupResolver implements GroupResolver {

    private final String attribute;

    public MemberOfGroupResolver(String attribute) {
        this.attribute = attribute;
    }

    @Override
    public Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry) {
        LOGGER.trace("resolve groups from ldap attribute {}", attribute);
        Set<String> groups = new HashSet<>();
        var ldapAttribute = ldapEntry.getAttribute(attribute);

        LOGGER.trace("ldap-attribute: {}", ldapAttribute);

        if (ldapAttribute != null && !ldapAttribute.isBinary()) {
            for (String value : ldapAttribute.getStringValues()) {
                String group = normalizeName(value);
                LOGGER.trace("added group {} to attribute map", group);
                groups.add(group);
            }
        } else {
            LOGGER.debug("could not find text based group attribute {} at {}", attribute, ldapEntry.getDn());
        }
        return groups;
    }

    private String normalizeName(String value) {
        return extractNameFromDn(value);
    }

    private String extractNameFromDn(String dn) {
        String result = dn;
        int eqindex = dn.indexOf('=');
        int coindex = dn.indexOf(',');
        if (eqindex > 0 && (coindex < 0 || eqindex < coindex) && dn.length() > eqindex + 1) {
            dn = dn.substring(eqindex + 1);
            coindex = dn.indexOf(',');
            if (coindex > 0) {
                result = dn.substring(0, coindex);
            } else {
                result = dn;
            }
        }
        return result;
    }

}
