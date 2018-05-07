package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class TimedLoginLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(TimedLoginLimiter.class);

    private final TimedLoginLimiterConfiguration configuration;
    private final Clock clock;

    private Map<String, AccountLog> accountLogs = createLruMap();

    private Map<String, AccountLog> createLruMap() {
        return new LinkedHashMap<String, AccountLog>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > configuration.maxAccounts();
            }
        };
    }

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
                evaluateAccountLog(account, accountLog);
            }
        }
    }

    private void evaluateAccountLog(String account, AccountLog accountLog) throws AuthenticationException {
        if (accountLog.failureCount >= configuration.getMaxNumber()) {
            if (inLockTime(accountLog)) {
                LOG.info("Rejected account due to too many failed login attempts: " + account);
                throw new AuthenticationException(Collections.singletonMap("TimedLoginLimiter", AccountTemporarilyLockedException.class));
            } else {
                removeLog(account);
            }
        } else if (lastFailureAfterStoreTime(accountLog)) {
            removeLog(account);
        }
    }

    private void removeLog(String account) {
        accountLogs.remove(account);
    }

    private boolean inLockTime(AccountLog accountLog) {
        return clock.instant().isBefore(accountLog.lastLoginAttempt.plusSeconds(configuration.getLockTime()));
    }

    private boolean lastFailureAfterStoreTime(AccountLog accountLog) {
        return clock.instant().isAfter(accountLog.lastLoginAttempt.plusSeconds(configuration.getFailureStoreTime()));
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
