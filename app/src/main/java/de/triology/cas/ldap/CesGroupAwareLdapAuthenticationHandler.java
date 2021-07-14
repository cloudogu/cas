package de.triology.cas.ldap;

import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.security.auth.login.LoginException;
import java.util.*;

public class CesGroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesGroupAwareLdapAuthenticationHandler.class);

    private String groupAttribute;
    private GroupResolver groupResolver;

    /**
     * Creates a new authentication handler that delegates to the given authenticator.
     *
     * @param name             the name
     * @param servicesManager  the services manager
     * @param principalFactory the principal factory
     * @param order            the order
     * @param authenticator    Ldaptive authenticator component.
     * @param strategy         the strategy
     * @param groupAttribute   the name of the group attribute
     * @param groupResolver    the resolver for resolving groups
     */
    public CesGroupAwareLdapAuthenticationHandler(String name, ServicesManager servicesManager,
                                                  PrincipalFactory principalFactory, Integer order,
                                                  Authenticator authenticator,
                                                  AuthenticationPasswordPolicyHandlingStrategy strategy,
                                                  String groupAttribute, GroupResolver groupResolver) {
        super(name, servicesManager, principalFactory, order, authenticator, strategy);

        this.groupAttribute = groupAttribute;
        this.groupResolver = groupResolver;
        LOGGER.trace("{} created with group attribute {} and group resolver {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), groupAttribute, groupResolver);
    }

    @Override
    protected Principal createPrincipal(String username, LdapEntry ldapEntry) throws LoginException {
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
    protected Principal attachGroups(Principal principal, LdapEntry ldapEntry) {
        Map<String, List<Object>> attributes = new LinkedHashMap<>(principal.getAttributes());
        List<Object> groups = new ArrayList<>(groupResolver.resolveGroups(principal, ldapEntry));
        LOGGER.debug("adding groups {} to user attributes", groups);
        attributes.put(groupAttribute, groups);

        return principalFactory.createPrincipal(principal.getId(), attributes);
    }
}
