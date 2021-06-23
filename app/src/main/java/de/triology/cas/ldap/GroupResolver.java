/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

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
   * 
   * @return set of group namess
   */
  Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry);
}
