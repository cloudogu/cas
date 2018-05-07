package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class TimedLoginLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLoginLimiter.class);

    private final TimedLoginLimiterConfiguration configuration;
    private final Clock clock;

    private Map<String, AccountLog> accountLogs = new HashMap<>();

    TimedLoginLimiter(TimedLoginLimiterConfiguration configuration) {
        this(configuration, Clock.systemDefaultZone());
    }

    TimedLoginLimiter(TimedLoginLimiterConfiguration configuration, Clock clock) {
        this.configuration = configuration;
        this.clock = clock;
    }

    void assertNotLocked(String account) throws AuthenticationException {
        if (isLimitingEnabled()) {
            AccountLog accountLog = accountLogs.get(account);
            if (accountLog != null) {
                if (accountLog.failureCount >= configuration.getMaxNumber()) {
                    if (clock.instant().isBefore(accountLog.lastLoginAttempt.plusSeconds(configuration.getLockTime()))) {
                        LOG.info("Rejected account due to too many failed login attempts: " + account);
                        throw new AuthenticationException(Collections.singletonMap("TimedLoginLimiter", AccountTemporarilyLockedException.class));
                    } else {
                        accountLogs.remove(account);
                    }
                } else if (clock.instant().isAfter(accountLog.lastLoginAttempt.plusSeconds(configuration.getFailureStoreTime()))) {
                    accountLogs.remove(account);
                }
            }
        }
    }

    void loginFailed(String account) {
        if (isLimitingEnabled()) {
            AccountLog accountLog = accountLogs.get(account);
            if (accountLog == null) {
                accountLogs.put(account, new AccountLog(clock.instant(), 1));
            } else {
                accountLogs.put(account, new AccountLog(clock.instant(), accountLog.failureCount + 1));
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
    }
}
