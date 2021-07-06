/**
 * Copyright (c) 2015 TRIOLOGY GmbH. All Rights Reserved.
 * 
 * Copyright notice
 */

package de.triology.cas.ldap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConnectionAwareAuthenticator}.
 * 
 * @author Sebastian Sdorra
 */
@RunWith(MockitoJUnitRunner.class)
public class ConnectionAwareAuthenticatorTest {

    @Mock
    private DnResolver dnResolver;
    
    @Mock
    private AuthenticationHandler authenticationHandler;

    @Mock
    private AuthenticationHandlerResponse response;
    
    @Mock
    private AuthenticationCriteria criteria;

    /**
     * Tests {@link ConnectionAwareAuthenticator#resolveEntry(AuthenticationCriteria, AuthenticationHandlerResponse)}
     * with usage of system connection.
     * 
     * @throws LdapException 
     */
    @Test
    public void testResolveEntry() throws LdapException {
        when(response.isSuccess()).thenReturn(Boolean.TRUE);
        
        ConnectionAwareAuthenticator authenticator = createAuthenticator();
        
        EntryResolver entryResolver = mock(EntryResolver.class);
        authenticator.setEntryResolver(entryResolver);
        
        LdapEntry entry = new LdapEntry();
        when(entryResolver.resolve(criteria, response)).thenReturn(entry);
        
        LdapEntry resolvedEntry = authenticator.resolveEntry(criteria, response);
        assertSame(entry, resolvedEntry);
    }

    private ConnectionAwareAuthenticator createAuthenticator() {
        return new ConnectionAwareAuthenticator(dnResolver, authenticationHandler);
    }
}