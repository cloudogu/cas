/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 *
 * Copyright notice
 */
package de.triology.cas.ldap;

import org.ldaptive.*;
import org.ldaptive.auth.*;

import javax.validation.constraints.NotNull;

/**
 * Authenticator which is able to resolve attributes with the user connection or with the system connection.
 */
public class ConnectionAwareAuthenticator extends Authenticator {

    private static final EntryResolver NOOP_RESOLVER = new NoOpEntryResolver();
    
    private final boolean useUserConnectionToFetchAttributes;
    
    @NotNull
    private final ConnectionFactory connectionFactory;

    public ConnectionAwareAuthenticator(
            ConnectionFactory connectionFactory, DnResolver resolver, AuthenticationHandler handler, 
            boolean useUserConnectionToFetchAttributes
    ) {
        super(resolver, handler);
        this.connectionFactory = connectionFactory;
        this.useUserConnectionToFetchAttributes = useUserConnectionToFetchAttributes;
    }

    @Override
    protected LdapEntry resolveEntry(
            AuthenticationCriteria criteria, AuthenticationHandlerResponse response
    ) throws LdapException {
        LdapEntry entry = null;
        
        if (isResolvingAttributesRequired(response)) {
            // TODO
            EntryResolver resolver = getEntryResolver(null);
            entry = resolveEntry(response, criteria, resolver);
        }
        
        if (entry == null) {
            entry = resolveEntry(response, criteria, NOOP_RESOLVER);
        }
        return entry;
    }
    
    private boolean isResolvingAttributesRequired(AuthenticationHandlerResponse response) {
        return getResolveEntryOnFailure(); // TODO || response.getResult();
    }
    
    private EntryResolver getEntryResolver(AuthenticationRequest request) {
        EntryResolver resolver = getEntryResolver();
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
        
        LdapEntry entry;
        if (useUserConnectionToFetchAttributes) {
            entry = resolveEntryWithUserConnection(response, criteria, resolver);
        } else {
            entry = resolveEntryWithSystemConnection(criteria, resolver, response);
        }
        
        logger.trace("resolved entry={} with resolver={}", entry, resolver);
        return entry;
    }
    
    private LdapEntry resolveEntryWithUserConnection(AuthenticationHandlerResponse response, AuthenticationCriteria criteria, EntryResolver resolver) throws LdapException{
        logger.debug("use user connection to fetch attributes");
        return resolver.resolve(criteria, response);
    }
    
    private LdapEntry resolveEntryWithSystemConnection(AuthenticationCriteria criteria, EntryResolver resolver, AuthenticationHandlerResponse response) throws LdapException {
        logger.debug("use system connection to fetch attributes");

        return resolver.resolve(criteria, response);
    }
   
}
