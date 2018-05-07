package de.triology.cas.limiting;

class TimedLoginLimiterConfiguration {

    private final int maxNumber;
    private final int failureStoreTime;
    private final int lockTime;

    TimedLoginLimiterConfiguration(int maxNumber, int failureStoreTime, int lockTime) {
        validateParameters(maxNumber, failureStoreTime, lockTime);
        this.maxNumber = maxNumber;
        this.failureStoreTime = failureStoreTime;
        this.lockTime = lockTime;
    }

    private void validateParameters(int maxNumber, int failureStoreTime, int lockTime) {
        if (maxNumber > 0) {
            if (failureStoreTime <= 0) {
                throw new IllegalArgumentException("failureStoreTime has to be > 0 when maxNumber is set to value > 0");
            }
            if (lockTime <= 0) {
                throw new IllegalArgumentException("lockTime has to be > 0 when maxNumber is set to value > 0");
            }
        }
    }

    int getMaxNumber() {
        return maxNumber;
    }

    int getFailureStoreTime() {
        return failureStoreTime;
    }

    int getLockTime() {
        return lockTime;
    }
}
