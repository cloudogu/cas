package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class TimedLoginLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLoginLimiter.class);

    private final TimedLoginLimiterConfiguration configuration;
    private final Clock clock;

    private Map<String, AtomicReference<AccountLog>> accountLogs = new HashMap<>();

    TimedLoginLimiter(TimedLoginLimiterConfiguration configuration) {
        this(configuration, Clock.systemDefaultZone());
    }

    TimedLoginLimiter(TimedLoginLimiterConfiguration configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }

    void assertNotLocked(String account) throws AuthenticationException {
        if (isLimitingEnabled()) {
            AtomicReference<AccountLog> accountLogReference = accountLogs.get(account);
            if (accountLogReference != null && accountLogReference.get().isLocked()) {
                LOG.info("Rejected account due to too many failed login attempts: " + account);
                throw new AuthenticationException(Collections.singletonMap("TimedLoginLimiter", AccountTemporarilyLockedException.class));
            }
        }
    }

    void loginFailed(String account) {
        if (isLimitingEnabled()) {
            AtomicReference<AccountLog> accountLogReference = accountLogs.get(account);
            if (accountLogReference == null) {
                accountLogs.put(account, new AtomicReference<>(new AccountLog(clock.instant(), 1)));
            } else {
                accountLogReference.getAndUpdate(log -> new AccountLog(clock.instant(), log.failureCount + 1));
            }
        }
    }

    private boolean isLimitingEnabled() {
        return configuration.getMaxNumber() > 0;
    }

    private class AccountLog {
        private final Instant lastLoginAttempt;
        private final int failureCount;

        private AccountLog(Instant lastLoginAttempt, int failureCount) {
            this.lastLoginAttempt = lastLoginAttempt;
            this.failureCount = failureCount;
        }

        private boolean isLocked() {
            return failureCount > configuration.getMaxNumber()
                    && clock.instant().isBefore(lastLoginAttempt.plusSeconds(configuration.getFailureStoreTime()));
        }
    }
}
