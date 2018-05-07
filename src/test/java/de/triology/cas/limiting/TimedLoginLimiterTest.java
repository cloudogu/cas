package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TimedLoginLimiterTest {

    private static final TimedLoginLimiterConfiguration DEFAULT_TEST_CONFIGURATION = new TimedLoginLimiterConfiguration(2, 0, 0);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void accountShouldNotBeLocked_whenLockingIsDisabled() throws AuthenticationException {
        TimedLoginLimiterConfiguration disabledConfiguration = new TimedLoginLimiterConfiguration(0, 0, 0);
        TimedLoginLimiter timedLoginLimiter = new TimedLoginLimiter(disabledConfiguration);

        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.assertNotLocked("account");
    }

    @Test
    public void accountShouldNotBeLocked_atFirstLogin() throws AuthenticationException {
        new TimedLoginLimiter(DEFAULT_TEST_CONFIGURATION).assertNotLocked("account");
    }

    @Test
    public void accountShouldBeLocked_afterMaxNumberExceeded() throws AuthenticationException {
        TimedLoginLimiter timedLoginLimiter = new TimedLoginLimiter(DEFAULT_TEST_CONFIGURATION);
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.assertNotLocked("account");
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.assertNotLocked("account");
        timedLoginLimiter.loginFailed("account");

        expectedException.expect(AuthenticationException.class);

        timedLoginLimiter.assertNotLocked("account");
    }

    @Test
    public void accountShouldNotBeLocked_afterDifferentAccountIsLocked() throws AuthenticationException {
        TimedLoginLimiter timedLoginLimiter = new TimedLoginLimiter(DEFAULT_TEST_CONFIGURATION);
        timedLoginLimiter.loginFailed("different");
        timedLoginLimiter.loginFailed("different");
        timedLoginLimiter.loginFailed("different");

        timedLoginLimiter.assertNotLocked("account");
    }
}
