package de.triology.cas.ldap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.*;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

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

    @Test
    public void resolveEntry() throws LdapException {
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