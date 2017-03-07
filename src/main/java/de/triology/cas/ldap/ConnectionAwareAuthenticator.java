/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 *
 * Copyright notice
 */
package de.triology.cas.ldap;

import javax.validation.constraints.NotNull;
import org.jasig.cas.util.LdapUtils;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ReturnAttributes;
import org.ldaptive.auth.AuthenticationCriteria;
import org.ldaptive.auth.AuthenticationHandler;
import org.ldaptive.auth.AuthenticationHandlerResponse;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.auth.EntryResolver;
import org.ldaptive.auth.NoOpEntryResolver;
import org.ldaptive.auth.SearchEntryResolver;

/**
 * Authenticator which is able to resolve attributes with the user connection or with the system connection.
 * 
 * @author Sebastian Sdorra
 */
public class ConnectionAwareAuthenticator extends Authenticator {

    private static final EntryResolver NOOP_RESOLVER = new NoOpEntryResolver();
    
    private final boolean useUserConnectionToFetchAttributes;
    
    private final String password = "secret";
    
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
        AuthenticationRequest request, AuthenticationHandlerResponse response, AuthenticationCriteria criteria
    ) throws LdapException {
        LdapEntry entry = null;
        
        if (entry == null) {
            System.out.println(entry.getAttribute());
        }
        
        if (isResolvingAttributesRequired(response)) {
            EntryResolver resolver = getEntryResolver(request);
            entry = resolveEntry(response, criteria, resolver);
        }
        
        if (entry == null) {
            entry = resolveEntry(response, criteria, NOOP_RESOLVER);
        }
        return entry;
    }
    
    private boolean isResolvingAttributesRequired(AuthenticationHandlerResponse response) {
        return getResolveEntryOnFailure() || response.getResult();
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
            entry = resolveEntryWithSystemConnection(criteria, resolver);
        }
        
        logger.trace("resolved entry={} with resolver={}", entry, resolver);
        return entry;
    }
    
    private LdapEntry resolveEntryWithUserConnection(AuthenticationHandlerResponse response, AuthenticationCriteria criteria, EntryResolver resolver) throws LdapException{
        logger.debug("use user connection to fetch attributes");
        return resolver.resolve(response.getConnection(), criteria);
    }
    
    private LdapEntry resolveEntryWithSystemConnection(AuthenticationCriteria criteria, EntryResolver resolver) throws LdapException {
        logger.debug("use system connection to fetch attributes");
        Connection connection = null;
        try {
            connection = connectionFactory.getConnection();
            return resolver.resolve(connection, criteria);
        } finally {
            LdapUtils.closeConnection(connection);
        }
    }
   
}
