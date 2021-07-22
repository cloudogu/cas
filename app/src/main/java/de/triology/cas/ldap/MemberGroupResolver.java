/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * <p>
 * Copyright notice
 */
package de.triology.cas.ldap;


import org.apache.commons.lang.StringUtils;
import org.apereo.cas.authentication.principal.Principal;

import org.ldaptive.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
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

    /**
     * Search controls.
     */
    private final SearchControls searchControls;

    /**
     * LDAP connection factory.
     */
    private final ConnectionFactory connectionFactory;

    /**
     * LDAP search filter.
     */
    private final String searchFilter;

    /**
     * LDAP search scope.
     */
    private SearchScope searchScope;

     /**
     * LDAP group name attribute.
     */
    private String nameAttribute = "cn";


    public MemberGroupResolver(String baseDN, SearchControls searchControls, ConnectionFactory connectionFactory, String searchFilter) {
        this.baseDN = baseDN;
        this.searchControls = searchControls;
        this.connectionFactory = connectionFactory;
        this.searchFilter = searchFilter;
    }

    /**
     * Initializes the object after properties are set.
     */
    @PostConstruct
    public void initialize() {
        for (final SearchScope scope : SearchScope.values()) {
            if (scope.ordinal() == this.searchControls.getSearchScope()) {
                this.searchScope = scope;
            }
        }
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
        final SearchResponse response;
        try {
            response = new SearchOperation(connectionFactory).execute(createRequest(filter));
        } catch (final LdapException e) {
            throw new RuntimeException("Failed executing LDAP query " + filter, e);
        }
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
        request.setSearchScope(this.searchScope);
        request.setSizeLimit(Math.toIntExact(this.searchControls.getCountLimit()));
        request.setTimeLimit(Duration.ofMillis(this.searchControls.getTimeLimit()));
        return request;
    }

}
