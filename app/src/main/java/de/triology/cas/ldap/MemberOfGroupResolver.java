/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import org.apereo.cas.authentication.principal.Principal;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolve groups by reading an attribute from the ldap entry. <strong>Note</strong> you
 * have to add the name of the attribute to the additionalAttributes list of 
 * {@link GroupAwareLdapAuthenticationHandler}.
 */
public class MemberOfGroupResolver implements GroupResolver {
    
    private static final Logger LOG = LoggerFactory.getLogger(MemberOfGroupResolver.class);

    private final String attribute;
    private final boolean isDnAttribute;

    /**
     * Creates a new instance.
     * 
     * @param attribute name of memberOf attribute
     * @param isDnAttribute {@code true} if the attribute is an dn
     */
    public MemberOfGroupResolver(String attribute, boolean isDnAttribute) {
        this.attribute = attribute;
        this.isDnAttribute = isDnAttribute;
    }
    
    @Override
    public Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry) {
        LOG.trace("resolve groups from ldap attribute {}", attribute);
        Set<String> groups = new HashSet<>();
        LdapAttribute ldapAttribute = ldapEntry.getAttribute(attribute);
        if (ldapAttribute != null && !ldapAttribute.isBinary()) {
            for (String value : ldapAttribute.getStringValues()) {
                String group = normalizeName(value);
                LOG.trace("added group {} to attribute map", group);
                groups.add( group );
            }
        } else {
            LOG.debug("could not find text based group attribute {} at {}", attribute, ldapEntry.getDn());
        }
        return groups;
    }
    
    private String normalizeName(String value) {
        if (isDnAttribute){
          return extractNameFromDn(value);  
        }
        return value;
    }
    
    private String extractNameFromDn(String dn) {
        String result = dn;
        int eqindex = dn.indexOf('=');
        int coindex = dn.indexOf(',');
        if ( eqindex > 0 && (coindex < 0 || eqindex < coindex) && dn.length() > eqindex + 1 ){
            dn = dn.substring(eqindex + 1);
            coindex = dn.indexOf(',');
            if (coindex > 0){
                result = dn.substring(0, coindex);
            } else {
                result = dn;
            }
        }
        return result;
    }

}
