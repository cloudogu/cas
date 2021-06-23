package de.triology.cas.limiting;

import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static de.triology.cas.limiting.CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapter.isTimeLocked;
import static org.junit.Assert.assertTrue;

public class CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapterTest {

    @Test
    public void test_static_isTimeLocked_successfully() throws InterruptedException {
        ZonedDateTime old = ZonedDateTime.now(ZoneOffset.UTC);
        Thread.sleep(100);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        boolean isLocked = isTimeLocked(now, old, 2);
        assertTrue(isLocked);
        isLocked = isTimeLocked(now, old, 0);
        assertTrue(!isLocked);
    }
    @Test
    public void test_static_isTimeLocked_timeout() throws InterruptedException {
        ZonedDateTime old = ZonedDateTime.now(ZoneOffset.UTC);
        Thread.sleep(1100);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        boolean isLocked = isTimeLocked(now, old, 1);
        assertTrue(!isLocked);
    }
}