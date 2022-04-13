package de.triology.cas.ldap.resolvers;

import org.apereo.cas.authentication.principal.Principal;
import org.ldaptive.LdapEntry;

import java.util.Set;

/**
 * Resolves groups for an principal.
 */
public interface GroupResolver {

    /**
     * Returns a set of resolved group names.
     *
     * @param principal principal
     * @param ldapEntry ldap entry
     * @return set of group names
     */
    Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry);
}
