/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.ldap;

import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.*;

/**
 * Ldap authentication handler which appends resolved groups to the principal attributes.
 * Groups will be resolved with a {@link GroupResolver} and will be attached with the
 * configured name.
 * 
 * @author Sebastian Sdorra
 */
public final class GroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GroupAwareLdapAuthenticationHandler.class);

  private GroupResolver groupResolver;

  private String groupAttribute = "groups";

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
  public GroupAwareLdapAuthenticationHandler(String name, ServicesManager servicesManager, PrincipalFactory principalFactory, Integer order, Authenticator authenticator, AuthenticationPasswordPolicyHandlingStrategy strategy) {
    super(name, servicesManager, principalFactory, order, authenticator, strategy);
  }


  /**
   * Sets the name of the group principal attribute. The default is "groups".
   * 
   * @param groupAttribute name of group principal attribute.
   */
  public void setGroupAttribute(String groupAttribute) {
    this.groupAttribute = groupAttribute;
  }

  /**
   * Sets the resolver for groups.
   * 
   * @param groupResolver groups resolver
   */
  public void setGroupResolver(GroupResolver groupResolver) {
    this.groupResolver = groupResolver;
  }

  @Override
  protected Principal createPrincipal(String username, LdapEntry ldapEntry) throws LoginException {
    var principal = super.createPrincipal(username, ldapEntry);
    
    if ( groupResolver != null ) {
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
   * 
   * @return new principal with groups attribute
   */
  protected Principal attachGroups(Principal principal, LdapEntry ldapEntry) {
    Map<String, List<Object>> attributes = new LinkedHashMap<>(principal.getAttributes());
    List<Object> groups = new ArrayList<>(groupResolver.resolveGroups(principal, ldapEntry));
    LOG.debug("adding groups {} to user attributes", groups);
    attributes.put(groupAttribute, groups);

    var factory = new DefaultPrincipalFactory();
    return factory.createPrincipal(principal.getId(), attributes);
  }
}
