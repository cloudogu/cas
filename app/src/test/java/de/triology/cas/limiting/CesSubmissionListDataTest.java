package de.triology.cas.limiting;

import junit.framework.TestCase;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class CesSubmissionListDataTest extends TestCase {

    private ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    @Test
    public void test_SubmissionData_constructor(){
        // when
        CesSubmissionListData data = new CesSubmissionListData();

        // then
        assertEquals(0, data.getFailedSubmissions());
        assertNull(data.getFirstSubmissionFailure());
        assertNull(data.getLockTime());
    }

    @Test
    public void test_SubmissionData_reset(){
        // given
        CesSubmissionListData data = new CesSubmissionListData();
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordHostLock(getCurrentTime());

        // when
        data.reset();

        // then
        assertEquals(0, data.getFailedSubmissions());
        assertNull(data.getFirstSubmissionFailure());
        assertNull(data.getLockTime());
    }

    @Test
    public void test_SubmissionData_recordFailedSubmission(){
        // given
        CesSubmissionListData data = new CesSubmissionListData();

        // when
        ZonedDateTime first = getCurrentTime();
        data.recordFailedSubmission(first);

        // then
        assertEquals(1, data.getFailedSubmissions());
        assertEquals(first, data.getFirstSubmissionFailure());
        assertNull(data.getLockTime());

        // when
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());
        data.recordFailedSubmission(getCurrentTime());

        // then
        assertEquals(5, data.getFailedSubmissions());
        assertEquals(first, data.getFirstSubmissionFailure());
        assertNull(data.getLockTime());
    }

    @Test
    public void test_SubmissionData_recordHostLock(){
        // given
        CesSubmissionListData data = new CesSubmissionListData();

        // when
        ZonedDateTime lockTime = getCurrentTime();
        data.recordHostLock(lockTime);

        // then
        assertEquals(lockTime, data.getLockTime());
    }
}