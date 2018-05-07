package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimedLoginLimiterTest {

    private static final TimedLoginLimiterConfiguration DEFAULT_TEST_CONFIGURATION = new TimedLoginLimiterConfiguration(2, 10, 10);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Clock clock = mock(Clock.class);

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

    @Test
    public void lastFailureShouldBeAccountedUpToConfiguredTime() throws AuthenticationException {
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(start);

        TimedLoginLimiter timedLoginLimiter = new TimedLoginLimiter(DEFAULT_TEST_CONFIGURATION, clock);
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.loginFailed("account");

        expectedException.expect(AuthenticationException.class);

        when(clock.instant()).thenReturn(start.plusSeconds(9));
        timedLoginLimiter.assertNotLocked("account");
    }

    @Test
    public void lastFailureShouldBeIgnoredAfterConfiguredTime() throws AuthenticationException {
        Instant start = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        when(clock.instant()).thenReturn(start);

        TimedLoginLimiter timedLoginLimiter = new TimedLoginLimiter(DEFAULT_TEST_CONFIGURATION, clock);
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.loginFailed("account");
        timedLoginLimiter.loginFailed("account");

        when(clock.instant()).thenReturn(start.plusSeconds(11));
        timedLoginLimiter.assertNotLocked("account");
    }
}
