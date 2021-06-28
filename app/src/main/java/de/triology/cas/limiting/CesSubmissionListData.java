package de.triology.cas.limiting;

import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * Data class to hold information about failed submission
 */
@Getter
public class CesSubmissionListData {
    private int failedSubmissions;
    private ZonedDateTime firstSubmissionFailure;
    private ZonedDateTime lockTime;

    public CesSubmissionListData() {
        reset();
    }

    public void reset() {
        failedSubmissions = 0;
        firstSubmissionFailure = null;
        lockTime = null;
    }

    public void recordFailedSubmission(ZonedDateTime now) {
        if (firstSubmissionFailure == null) {
            firstSubmissionFailure = now;
        }
        failedSubmissions++;
    }

    public void recordHostLock(ZonedDateTime now) {
        lockTime = now;
    }

    @Override
    public String toString() {
        return "CesSubmissionListData{" +
                "failedSubmissions=" + failedSubmissions +
                ", firstSubmissionFailure=" + firstSubmissionFailure +
                ", lockTime=" + lockTime +
                '}';
    }
}