/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.ldap;

import org.jasig.cas.authentication.LdapAuthenticationHandler;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;
import org.ldaptive.LdapEntry;
import org.ldaptive.auth.Authenticator;

import javax.security.auth.login.LoginException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ldap authentication handler which appends resolved groups to the principal attributes.
 * Groups will be resolved with a {@link GroupResolver} and will be attached with the
 * configured name.
 * 
 * @author Sebastian Sdorra
 */
public final class GroupAwareLdapAuthenticationHandler extends LdapAuthenticationHandler {
  
  private GroupResolver groupResolver;

  private String groupAttribute = "groups";
  
  public GroupAwareLdapAuthenticationHandler(Authenticator authenticator) {
    super(authenticator);
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
    Principal principal = super.createPrincipal(username, ldapEntry);
    
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
    Map<String,Object> attributes = new LinkedHashMap<>(principal.getAttributes());
    Set<String> groups = groupResolver.resolveGroups(principal, ldapEntry);
    logger.debug("adding groups {} to user attributes", groups);
    attributes.put(groupAttribute, groups);
    
    return new SimplePrincipal(principal.getId(), attributes);
  }
}
