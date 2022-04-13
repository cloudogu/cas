package de.triology.cas.ldap.resolvers;

import org.apache.commons.lang.StringUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.ldaptive.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.SearchControls;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolves groups by searching the directory server for a member attribute, which contains the dn of the ldap entry.
 */
public class MemberGroupResolver implements GroupResolver {

    private static final Logger LOG = LoggerFactory.getLogger(MemberGroupResolver.class);

    private final String baseDN;
    private final SearchControls searchControls;
    private SearchScope searchScope;
    private final ConnectionFactory connectionFactory;
    private final String searchFilter;
    private final String nameAttribute = "cn";


    public MemberGroupResolver(String baseDN, ConnectionFactory connectionFactory, String searchFilter) {
        this.baseDN = baseDN;
        this.searchControls = new SearchControls();
        this.searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        this.searchScope = SearchScope.SUBTREE;

        this.connectionFactory = connectionFactory;
        this.searchFilter = searchFilter;
    }

    @Override
    public Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry) {
        if (StringUtils.isEmpty(searchFilter)) {
            LOG.trace("skip resolving groups for member, because of missing search filter");
            return Collections.emptySet();
        }
        LOG.debug("resolve groups for {}", ldapEntry.getDn());
        FilterTemplate filter = createFilter(principal, ldapEntry);
        return resolveGroupsByLdapFilter(filter);
    }

    private Set<String> resolveGroupsByLdapFilter(FilterTemplate filter) {
        LOG.trace("resolveGroupsByLdapFilter");
        final SearchResponse response;
        try {
            SearchRequest request = createRequest(filter);
            LOG.trace("resolveGroupsByLdapFilter - filter: {}", filter);
            LOG.trace("resolveGroupsByLdapFilter - request: {}", request);
            response = new SearchOperation(connectionFactory).execute(createRequest(filter));
        } catch (final LdapException e) {
            LOG.trace("resolveGroupsByLdapFilter - error: {}", e.getMessage());
            throw new RuntimeException("Failed executing LDAP query " + filter, e);
        }
        LOG.trace("got response {}", response);
        LOG.trace("got response entries{}", response.getEntries());
        final Set<String> groups = new HashSet<>();
        for (final LdapEntry entry : response.getEntries()) {
            String group = extractGroupName(entry);
            LOG.trace("added group {} to attribute map", group);
            groups.add(group);
        }
        return groups;

    }

    private String extractGroupName(LdapEntry entry) {
        return entry.getAttribute(nameAttribute).getStringValue();
    }

    FilterTemplate createFilter(Principal principal, LdapEntry ldapEntry) {
        return new FilterTemplate(searchFilter, new Object[]{ldapEntry.getDn(), principal.getId()});
    }

    private SearchRequest createRequest(final FilterTemplate filter) {
        final var request = new SearchRequest();
        request.setBaseDn(this.baseDN);
        request.setFilter(filter);
        request.setReturnAttributes(nameAttribute);
        request.setSearchScope(searchScope);
        request.setSizeLimit(Math.toIntExact(this.searchControls.getCountLimit()));
        request.setTimeLimit(Duration.ofMillis(this.searchControls.getTimeLimit()));
        return request;
    }

}
