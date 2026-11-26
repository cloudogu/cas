package de.triology.cas.ldap;

import de.triology.cas.ldap.resolvers.GroupResolver;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.auth.User;
import org.ldaptive.auth.Authenticator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Setter
@Slf4j
public class CesGroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {

    private static final String GROUP_ATTRIBUTE = "groups";

    private GroupResolver groupResolver;
    private final ConnectionFactory connectionFactory;

    /**
     * Creates a new authentication handler that delegates to the given authenticator.
     *
     * @param name             the name
     * @param principalFactory the principal factory
     * @param authenticator    Ldaptive authenticator component.
     * @param strategy         the strategy
     * @param groupResolver    the resolver for resolving groups
     */
    public CesGroupAwareLdapAuthenticationHandler(String name,
                                                  PrincipalFactory principalFactory,
                                                  Authenticator authenticator,
                                                  AuthenticationPasswordPolicyHandlingStrategy strategy,
                                                  GroupResolver groupResolver,
                                                  ConnectionFactory connectionFactory) {
        super(name, principalFactory, 0, authenticator, strategy);

        this.groupResolver = groupResolver;
        this.connectionFactory = connectionFactory;
        LOGGER.trace("{} created with group attribute {} and group resolver {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), GROUP_ATTRIBUTE, groupResolver);
    }

    public Principal resolvePrincipal(String username) throws Throwable {
        return createPrincipal(username, findUserEntry(username, getAuthenticatedEntryAttributes()));
    }

    public Optional<String> findTechnicalAccountDisplayName(String username) throws LdapException {
        LdapEntry entry = findUserEntry(username, "technical", CesInternalLdapUser.DisplayNameAttribute);
        var technical = entry.getAttribute("technical");
        if (technical == null || !"TRUE".equalsIgnoreCase(technical.getStringValue())) {
            return Optional.empty();
        }

        var displayName = entry.getAttribute(CesInternalLdapUser.DisplayNameAttribute);
        return Optional.ofNullable(displayName)
                .map(attribute -> attribute.getStringValue())
                .filter(value -> !value.isBlank())
                .or(() -> Optional.of(username));
    }

    private LdapEntry findUserEntry(String username, String... attributes) throws LdapException {
        String dn = getAuthenticator().resolveDn(new User(username));
        SearchRequest request = SearchRequest.objectScopeSearchRequest(dn, attributes);
        var response = new SearchOperation(connectionFactory).execute(request);
        if (response.getEntries().isEmpty()) {
            throw new IllegalStateException("No LDAP entry found for " + username);
        }
        return response.getEntries().iterator().next();
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
