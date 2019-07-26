/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 *
 * Copyright notice
 */
package de.triology.cas.ldap;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.naming.directory.SearchControls;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;

import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.util.LdapUtils;

import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.Response;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SearchScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves groups by searching the directory server for a member attribute, which contains the dn of the ldap entry.
 *
 * @author Sebastian Sdorra
 */
public class MemberGroupResolver implements GroupResolver {

  private static final Logger LOG = LoggerFactory.getLogger(MemberGroupResolver.class);

  @NotNull
  private String baseDN;

  /**
   * Search controls.
   */
  @NotNull
  private SearchControls searchControls;

  /**
   * LDAP connection factory.
   */
  @NotNull
  private ConnectionFactory connectionFactory;

  /**
   * LDAP search scope.
   */
  private SearchScope searchScope;

  /**
   * LDAP search filter.
   */
  @NotNull
  private String searchFilter;

  /**
   * LDAP group name attribute.
   */
  private String nameAttribute = "cn";

  /**
   * Sets the base DN of the LDAP search for groups.
   *
   * @param dn LDAP base DN of search.
   */
  public void setBaseDN(final String dn) {
    this.baseDN = dn;
  }

  /**
   * Sets the LDAP search filter used to query for groups.
   *
   * @param filter Search filter of the form "(member={0})" where {0} is replaced with the dn of the ldap entry,
   *               {1} is replaced with id of the principal.
   */
  public void setSearchFilter(final String filter) {
    this.searchFilter = filter;
  }

  /**
   * Sets a number of parameters that control LDAP search semantics including search scope, maximum number of results
   * retrieved, and search timeout.
   *
   * @param searchControls LDAP search controls.
   */
  public void setSearchControls(final SearchControls searchControls) {
    this.searchControls = searchControls;
  }

  /**
   * Sets the connection factory that produces LDAP connections on which searches occur. It is strongly recommended that
   * this be a <code>PooledConnecitonFactory</code> object.
   *
   * @param connectionFactory LDAP connection factory.
   */
  public void setConnectionFactory(final ConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  /**
   * Sets the name of the ldap attribute, which holds the name of the group. Default is "cn".
   * 
   * @param nameAttribute name attribute of group
   */
  public void setNameAttribute(String nameAttribute) {
    this.nameAttribute = nameAttribute;
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
    if ( StringUtils.isEmpty(searchFilter) ) {
      LOG.trace("skip resolving groups for member, because of missing search filter");
      return Collections.emptySet();
    }
    LOG.debug("resolve groups for {}", ldapEntry.getDn());
    SearchFilter filter = createFilter(principal, ldapEntry);
    return resolveGroupsByLdapFilter(filter);
  }

  private Set<String> resolveGroupsByLdapFilter(SearchFilter filter) {
    Connection connection = null;
    try {
      try {
        connection = this.connectionFactory.getConnection();
      }
      catch (final LdapException e) {
        throw new RuntimeException("Failed getting LDAP connection", e);
      }
      final Response<SearchResult> response;
      try {
        response = new SearchOperation(connection).execute(createRequest(filter));
      }
      catch (final LdapException e) {
        throw new RuntimeException("Failed executing LDAP query " + filter, e);
      }
      final SearchResult result = response.getResult();
      final Set<String> groups = new HashSet<>();
      for (final LdapEntry entry : result.getEntries()) {
        String group = extractGroupName(entry);
        LOG.trace("added group {} to attribute map", group);
        groups.add(group);
      }
      return groups;
    }
    finally {
      LdapUtils.closeConnection(connection);
    }
  }

  private String extractGroupName(LdapEntry entry) {
    return entry.getAttribute(nameAttribute).getStringValue();
  }

  SearchFilter createFilter(Principal principal, LdapEntry ldapEntry) {
    return new SearchFilter(searchFilter, new Object[]{ldapEntry.getDn(), principal.getId()});
  }

  private SearchRequest createRequest(final SearchFilter filter) {
    final SearchRequest request = new SearchRequest();
    request.setBaseDn(this.baseDN);
    request.setSearchFilter(filter);
    request.setReturnAttributes(new String[]{nameAttribute});
    request.setSearchScope(this.searchScope);
    request.setSizeLimit(this.searchControls.getCountLimit());
    request.setTimeLimit(this.searchControls.getTimeLimit());
    return request;
  }

}
