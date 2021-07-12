package de.triology.cas.ldap;

import lombok.Getter;
import lombok.Setter;
import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.ldaptive.LdapEntry;
import org.ldaptive.ReturnAttributes;
import org.ldaptive.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.*;

@Getter
@Setter
public class CesGroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesGroupAwareLdapAuthenticationHandler.class);

    private String[] authenticatedEntryAttributes = ReturnAttributes.NONE.value();


    // TODO Use spring
    private String groupAttribute = "memberOf";


    /**
     * Creates a new authentication handler that delegates to the given authenticator.
     *
     * @param name             the name
     * @param servicesManager  the services manager
     * @param principalFactory the principal factory
     * @param order            the order
     * @param authenticator    Ldaptive authenticator component.
     * @param strategy         the strategy
     */
    public CesGroupAwareLdapAuthenticationHandler(String name, ServicesManager servicesManager, PrincipalFactory principalFactory, Integer order, Authenticator authenticator, AuthenticationPasswordPolicyHandlingStrategy strategy) {
        super(name, servicesManager, principalFactory, order, authenticator, strategy);
        LOGGER.trace("{} created", CesGroupAwareLdapAuthenticationHandler.class.getSimpleName());
    }

    @Override
    protected Principal createPrincipal(String username, LdapEntry ldapEntry) throws LoginException {
        LOGGER.trace("createPrincipal -> LdapEntry: {}", ldapEntry);
        var principal = super.createPrincipal(username, ldapEntry);
        LOGGER.trace("Principal {] " + principal);

        GroupResolver groupResolver = new MemberGroupResolver();
        LOGGER.error("Group resolver:" + groupResolver);
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
        List<GroupResolver> groupResolvers = new ArrayList<>(2);
        groupResolvers.add(new MemberGroupResolver());
        groupResolvers.add(new MemberOfGroupResolver());
        GroupResolver groupResolver = new CombinedGroupResolver(groupResolvers);

        Map<String, List<Object>> attributes = new LinkedHashMap<>(principal.getAttributes());
        List<Object> groups = new ArrayList<>(groupResolver.resolveGroups(principal, ldapEntry));
        LOGGER.error("adding groups {} to user attributes", groups);
        attributes.put(groupAttribute, groups);

        var factory = new DefaultPrincipalFactory();
        return factory.createPrincipal(principal.getId(), attributes);
    }
}