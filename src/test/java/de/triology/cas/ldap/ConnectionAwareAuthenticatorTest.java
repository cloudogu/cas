/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import org.junit.Test;
import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AuthenticationCriteria;
import org.ldaptive.auth.AuthenticationHandler;
import org.ldaptive.auth.AuthenticationHandlerResponse;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.DnResolver;
import org.ldaptive.auth.EntryResolver;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit tests for {@link ConnectionAwareAuthenticator}.
 * 
 * @author Sebastian Sdorra
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionAwareAuthenticatorTest {

    @Mock
    private ConnectionFactory connectionFactory;
    
    @Mock
    private DnResolver dnResolver;
    
    @Mock
    private AuthenticationHandler authenticationHandler;
    
    @Mock
    private AuthenticationRequest request;
    
    @Mock
    private AuthenticationHandlerResponse response;
    
    @Mock
    private AuthenticationCriteria criteria;

    @Mock
    private Connection userConnection;
    
    @Mock
    private Connection systemConnection;
   
    /**
     * Configure mocks.
     * 
     * @throws LdapException 
     */
    @Before
    public void setUpMocks() throws LdapException {
        when(response.getConnection()).thenReturn(userConnection);
        when(connectionFactory.getConnection()).thenReturn(systemConnection);
    }
    
    /**
     * Tests {@link ConnectionAwareAuthenticator#resolveEntry(AuthenticationRequest, AuthenticationHandlerResponse, AuthenticationCriteria)}
     * with usage of user connection.
     * 
     * @throws LdapException 
     */
    @Test
    public void testResolveEntryWithUserConnection() throws LdapException {
        when(response.getResult()).thenReturn(Boolean.TRUE);
        
        ConnectionAwareAuthenticator authenticator = createAuthenticator(true);
        
        EntryResolver entryResolver = mock(EntryResolver.class);
        authenticator.setEntryResolver(entryResolver);
        
        authenticator.resolveEntry(request, response, criteria);
        
        verify(entryResolver).resolve(userConnection, criteria);
        verify(entryResolver, never()).resolve(systemConnection, criteria);
    }
    
    /**
     * Tests {@link ConnectionAwareAuthenticator#resolveEntry(AuthenticationRequest, AuthenticationHandlerResponse, AuthenticationCriteria)}
     * with usage of system connection.
     * 
     * @throws LdapException 
     */
    @Test
    public void testResolveEntryWithSystemConnection() throws LdapException {
        when(response.getResult()).thenReturn(Boolean.TRUE);
        
        ConnectionAwareAuthenticator authenticator = createAuthenticator(false);
        
        EntryResolver entryResolver = mock(EntryResolver.class);
        authenticator.setEntryResolver(entryResolver);
        
        authenticator.resolveEntry(request, response, criteria);
        
        verify(entryResolver).resolve(systemConnection, criteria);
        verify(entryResolver, never()).resolve(userConnection, criteria);
    }

    private ConnectionAwareAuthenticator createAuthenticator( boolean useUserConnectionToFetchAttributes ) {
        return new ConnectionAwareAuthenticator(connectionFactory, dnResolver, authenticationHandler, useUserConnectionToFetchAttributes);
    }
}