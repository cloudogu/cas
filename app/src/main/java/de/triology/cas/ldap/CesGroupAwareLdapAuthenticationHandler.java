package de.triology.cas.ldap;

import de.triology.cas.ldap.resolvers.GroupResolver;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@Slf4j
public class CesGroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {

    private static final String GROUP_ATTRIBUTE = "groups";

    private GroupResolver groupResolver;

    /**
     * Creates a new authentication handler that delegates to the given authenticator.
     *
     * @param name             the name
     * @param servicesManager  the services manager
     * @param principalFactory the principal factory
     * @param authenticator    Ldaptive authenticator component.
     * @param strategy         the strategy
     * @param groupResolver    the resolver for resolving groups
     */
    public CesGroupAwareLdapAuthenticationHandler(String name, ServicesManager servicesManager,
                                                  PrincipalFactory principalFactory,
                                                  Authenticator authenticator,
                                                  AuthenticationPasswordPolicyHandlingStrategy strategy,
                                                  GroupResolver groupResolver) {
        super(name, servicesManager, principalFactory, 0, authenticator, strategy);

        this.groupResolver = groupResolver;
        LOGGER.trace("{} created with group attribute {} and group resolver {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), GROUP_ATTRIBUTE, groupResolver);
    }

    @Override
    protected Principal createPrincipal(String username, LdapEntry ldapEntry) throws Throwable {
        LOGGER.trace("createPrincipal from LdapEntry: {}", ldapEntry);
        var principal = super.createPrincipal(username, ldapEntry);
        LOGGER.trace("created Principal from super method is: {} ", principal);

        if (groupResolver != null) {
            // resolve and attach groups
            principal = attachGroups(principal, ldapEntry);
        }

        return principal;
    }

    /**
     * Resolves groups and creates a new principal with attached group attribute.
     *
     * @param principal principal
     * @param ldapEntry ldap entry
     * @return new principal with groups attribute
     */
    protected Principal attachGroups(Principal principal, LdapEntry ldapEntry) throws Throwable {
        Map<String, List<Object>> attributes = new LinkedHashMap<>(principal.getAttributes());
        List<Object> groups = new ArrayList<>(groupResolver.resolveGroups(principal, ldapEntry));
        LOGGER.debug("adding groups {} to user attributes", groups);
        attributes.put(GROUP_ATTRIBUTE, groups);

        return principalFactory.createPrincipal(principal.getId(), attributes);
    }
}
