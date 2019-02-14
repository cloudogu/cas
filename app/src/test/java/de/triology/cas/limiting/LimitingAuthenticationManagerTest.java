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

import java.util.Arrays;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class LimitingAuthenticationManagerTest {

    private static final String VALID_ACCOUNT_ID = "valid account";
    private static final String INVALID_ACCOUNT_ID = "invalid account";
    private static final String LOCKED_ACCOUNT_ID = "locked account";

    @Mock
    private AuthenticationManager delegate;
    @Mock
    private TimedLoginLimiter limiter;
    @InjectMocks
    private LimitingAuthenticationManager manager;

    private Credential validCredential = () -> VALID_ACCOUNT_ID;
    private Credential invalidCredential = () -> INVALID_ACCOUNT_ID;
    private Credential lockedCredential = () -> LOCKED_ACCOUNT_ID;

    private final Authentication validAuthentication = mock(Authentication.class);

    @Test
    public void shouldPassSuccessfulLogin() throws AuthenticationException {
        Authentication actualAuthentication = manager.authenticate(validCredential);

        assertSame("should return authentication from delegate", validAuthentication, actualAuthentication);
        verify(limiter, never()).loginFailed(VALID_ACCOUNT_ID);
    }

    @Test
    public void shouldReportFailedLogin() {
        try {
            manager.authenticate(invalidCredential);
            fail("authentication exception expected");
        } catch (AuthenticationException e) {
            verify(limiter).loginFailed(INVALID_ACCOUNT_ID);
        }
    }

    @Test
    public void shouldNotProceedLoginWhenLimiterFails() throws AuthenticationException {
        try {
            manager.authenticate(validCredential,lockedCredential);
            fail("authentication exception expected");
        } catch (AuthenticationException e) {
            verify(delegate, never()).authenticate(lockedCredential);
        }
    }

    @Before
    public void init() throws AuthenticationException {
        MockitoAnnotations.initMocks(this);

        when(delegate.authenticate(validCredential)).thenReturn(validAuthentication);

        when(delegate.authenticate(invalidCredential)).thenThrow(AuthenticationException.class);

        doThrow(AuthenticationException.class).when(limiter).assertNotLocked(containsAccountId(LOCKED_ACCOUNT_ID));
    }

    private String[] containsAccountId(String accountId) {
        return argThat(argument -> Arrays.stream(argument).anyMatch(accountId::equals));
    }
}
