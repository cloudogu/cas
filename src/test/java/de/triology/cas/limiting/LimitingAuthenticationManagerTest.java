package de.triology.cas.limiting;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.authentication.AuthenticationManager;
import org.jasig.cas.authentication.Credential;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class LimitingAuthenticationManagerTest {

    private static final String ACCOUNT_ID = "account";

    @Mock
    private AuthenticationManager delegate;
    @Mock
    private TimedLoginLimiter limiter;
    @InjectMocks
    private LimitingAuthenticationManager manager;

    private Credential credential = () -> ACCOUNT_ID;

    @Test
    public void shouldPassSuccessfulLogin() throws AuthenticationException {
        doNothing().when(limiter).assertNotLocked(ACCOUNT_ID);
        Authentication expectedAuthentication = mock(Authentication.class);
        when(delegate.authenticate(credential)).thenReturn(expectedAuthentication);

        Authentication actualAuthentication = manager.authenticate(credential);

        assertSame("should return authentication from delegate", expectedAuthentication, actualAuthentication);
        verify(limiter, never()).loginFailed(ACCOUNT_ID);
    }

    @Test
    public void shouldReportFailedLogin() throws AuthenticationException {
        doNothing().when(limiter).assertNotLocked(ACCOUNT_ID);
        when(delegate.authenticate(credential)).thenThrow(AuthenticationException.class);

        try {
            manager.authenticate(credential);
            fail("authentication exception expected");
        } catch (AuthenticationException e) {
            verify(limiter).loginFailed(ACCOUNT_ID);
        }
    }

    @Test
    public void shouldNotProceedLoginWhenLimiterFails() throws AuthenticationException {
        doThrow(AuthenticationException.class).when(limiter).assertNotLocked(ACCOUNT_ID);

        try {
            manager.authenticate(credential);
            fail("authentication exception expected");
        } catch (AuthenticationException e) {
            verify(delegate, never()).authenticate(credential);
        }
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }
}
