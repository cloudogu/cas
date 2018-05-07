package de.triology.cas.limiting;

class TimedLoginLimiterConfiguration {

    private final int maxNumber;
    private final int failureStoreTime;
    private final int lockTime;

    TimedLoginLimiterConfiguration(int maxNumber, int failureStoreTime, int lockTime) {
        this.maxNumber = maxNumber;
        this.failureStoreTime = failureStoreTime;
        this.lockTime = lockTime;
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
