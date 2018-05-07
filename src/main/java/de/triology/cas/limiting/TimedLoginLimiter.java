package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class TimedLoginLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLoginLimiter.class);

    private final TimedLoginLimiterConfiguration configuration;

    private Map<String, AtomicInteger> failureCounts = new HashMap<>();

    TimedLoginLimiter(TimedLoginLimiterConfiguration configuration) {
        this.configuration = configuration;
    }

    void assertNotLocked(String account) throws AuthenticationException {
        if (isLimitingEnabled() && getCounter(account).get() > configuration.getMaxNumber()) {
            LOG.info("Rejected account due to too many failed login attempts: " + account);
            throw new AuthenticationException(Collections.singletonMap("TimedLoginLimiter", AccountTemporarilyLockedException.class));
        }
    }

    void loginFailed(String account) {
        if (isLimitingEnabled()) {
            getCounter(account).incrementAndGet();
        }
    }

    private AtomicInteger getCounter(String account) {
        if (failureCounts.containsKey(account)) {
            return failureCounts.get(account);
        } else {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            failureCounts.put(account, atomicInteger);
            return atomicInteger;
        }
    }

    private boolean isLimitingEnabled() {
        return configuration.getMaxNumber() > 0;
    }
}
