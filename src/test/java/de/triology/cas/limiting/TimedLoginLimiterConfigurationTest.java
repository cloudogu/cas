package de.triology.cas.limiting;

import org.junit.Test;

public class TimedLoginLimiterConfigurationTest {

    @Test
    public void shouldAcceptValuesForDisabledConfiguration() {
        new TimedLoginLimiterConfiguration(0, 0, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectIllegalFailureStoreTimeWhenEnabled() {
        new TimedLoginLimiterConfiguration(1, 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectIllegalLockTimeWhenEnabled() {
        new TimedLoginLimiterConfiguration(1, 1, 0);
    }
}