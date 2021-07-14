/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolve groups by reading an attribute from the ldap entry. <strong>Note</strong> you
 * have to add the name of the attribute to the additionalAttributes list of 
 * {@link CesGroupAwareLdapAuthenticationHandler}.
 */
@Configuration("MemberOfGroupResolver")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class MemberOfGroupResolver implements GroupResolver {
    
    private static final Logger LOG = LoggerFactory.getLogger(MemberOfGroupResolver.class);

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.groups}")
    private String attribute;

    @Override
    public Set<String> resolveGroups(Principal principal, LdapEntry ldapEntry) {
        LOG.trace("resolve groups from ldap attribute {}", attribute);
        Set<String> groups = new HashSet<>();
        LdapAttribute ldapAttribute = ldapEntry.getAttribute(attribute);

        LOG.trace("ldap-attribute: {}", ldapAttribute);

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
          return extractNameFromDn(value);
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
