/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 *
 * Copyright notice
 */
package de.triology.cas.ldap;

import org.ldaptive.*;
import org.ldaptive.auth.*;

/**
 * Authenticator which is able to resolve attributes.
 */
public class ConnectionAwareAuthenticator extends Authenticator {

    private static final EntryResolver NOOP_RESOLVER = new NoOpEntryResolver();


    public ConnectionAwareAuthenticator(
            DnResolver resolver, AuthenticationHandler handler
    ) {
        super(resolver, handler);

    }

    @Override
    protected LdapEntry resolveEntry(
            AuthenticationCriteria criteria, AuthenticationHandlerResponse response
    ) throws LdapException {
        LdapEntry entry = null;
        
        if (isResolvingAttributesRequired(response)) {
            var resolver = getEntryResolver(criteria.getAuthenticationRequest());
            entry = resolveEntry(response, criteria, resolver);
        }
        
        if (entry == null) {
            entry = resolveEntry(response, criteria, NOOP_RESOLVER);
        }
        return entry;
    }
    
    private boolean isResolvingAttributesRequired(AuthenticationHandlerResponse response) {
        return getResolveEntryOnFailure() || response.isSuccess();
    }
    
    private EntryResolver getEntryResolver(AuthenticationRequest request) {
        var resolver = getEntryResolver();
        if (resolver == null) {
            if (ReturnAttributes.NONE.equalsAttributes(request.getReturnAttributes())) {
                resolver = NOOP_RESOLVER;
            } else {
                resolver = new SearchEntryResolver();
            }
        }
        return resolver;
    }
    
    private LdapEntry resolveEntry(
            AuthenticationHandlerResponse response, AuthenticationCriteria criteria, EntryResolver resolver
    ) throws LdapException {
        
        LdapEntry entry =  resolver.resolve(criteria, response);

        logger.trace("resolved entry={} with resolver={}", entry, resolver);
        return entry;
    }
}
