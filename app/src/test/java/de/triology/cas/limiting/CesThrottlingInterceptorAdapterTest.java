package de.triology.cas.limiting;

import org.apereo.cas.throttle.ThrottledRequestResponseHandler;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CesThrottlingInterceptorAdapterTest {

    private static final String CLIENT_ID = "127.0.0.1";

    private ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    public CesThrottlingInterceptorAdapter Setup(long max_number, long failure_store_time, long lockTime) {
        // general handler mock
        ThrottledRequestResponseHandler handlerMock = mock(ThrottledRequestResponseHandler.class);

        // general client info mock
        CesThrottlingInterceptorAdapter.IClientInfoProvider clientInfoMock = mock(CesThrottlingInterceptorAdapter.IClientInfoProvider.class);
        when(clientInfoMock.getClientInfo()).thenReturn(CLIENT_ID);

        ConcurrentMap<String, CesSubmissionListData> map = new ConcurrentHashMap<>();
        return new CesThrottlingInterceptorAdapter(
                map,
                handlerMock,
                clientInfoMock,
                max_number,
                failure_store_time,
                lockTime
        );
    }

    @Test
    public void test_shouldResponseBeRecordedAsFailure_noFailure() {
        // Given
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        when(responseMock.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 60, 60);

        // when
        boolean result = throttlingAdapter.shouldResponseBeRecordedAsFailure(responseMock);

        // then
        assertFalse(result);
    }

    @Test
    public void test_shouldResponseBeRecordedAsFailure_failure() {
        // Given
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        when(responseMock.getStatus()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 60, 60);

        // when
        boolean result = throttlingAdapter.shouldResponseBeRecordedAsFailure(responseMock);

        // then
        assertTrue(result);
    }

    @Test
    public void test_getTimeDiffInSeconds() throws InterruptedException {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 60, 60);
        ZonedDateTime time = getCurrentTime();
        Thread.sleep(550);
        ZonedDateTime time500 = getCurrentTime();
        Thread.sleep(500);
        ZonedDateTime time1050 = getCurrentTime();

        // when
        long diff500 = (long) throttlingAdapter.getTimeDiffInSeconds(time500, time);
        long diff1000 = (long) throttlingAdapter.getTimeDiffInSeconds(time1050, time);

        // then
        assertEquals(0, diff500);
        assertEquals(1, diff1000);
    }

    @Test
    public void test_invalidateLockIfRequired_do_not_invalidate() throws InterruptedException {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 30, 1);
        CesSubmissionListData data = new CesSubmissionListData();
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, data);
        throttlingAdapter.recordSubmissionFailure(null);

        Thread.sleep(500);

        // when
        throttlingAdapter.invalidateLockIfRequired(data);

        // then
        assertNotNull(data.getLockTime());
    }

    @Test
    public void test_invalidateLockIfRequired_do_invalidate() throws InterruptedException {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 30, 1);
        CesSubmissionListData data = new CesSubmissionListData();
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, data);
        throttlingAdapter.recordSubmissionFailure(null);

        Thread.sleep(1000);

        // when
        throttlingAdapter.invalidateLockIfRequired(data);

        // then
        assertNull(data.getLockTime());
    }

    @Test
    public void test_invalidateSubmissionDataIfRequired_invalidated() {
        // given
        CesSubmissionListData cesSubmissionListData = new CesSubmissionListData();

        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(2, 0, 60);
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, cesSubmissionListData);

        // when
        throttlingAdapter.recordSubmissionFailure(null);
        throttlingAdapter.invalidateSubmissionDataIfRequired(cesSubmissionListData);

        // then
        assertNull(cesSubmissionListData.getFirstSubmissionFailure());
        assertEquals(0, cesSubmissionListData.getFailedSubmissions());
    }

    @Test
    public void test_invalidateSubmissionDataIfRequired_notInvalidated() {
        // given
        CesSubmissionListData cesSubmissionListData = new CesSubmissionListData();

        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(2, 60, 60);
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, cesSubmissionListData);

        // when
        throttlingAdapter.recordSubmissionFailure(null);
        throttlingAdapter.invalidateSubmissionDataIfRequired(cesSubmissionListData);

        // then
        assertNotNull(cesSubmissionListData.getFirstSubmissionFailure());
        assertEquals(1, cesSubmissionListData.getFailedSubmissions());
    }

    @Test
    public void test_activateLockIfRequired_emptyInput() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(2, 30, 60);
        CesSubmissionListData data = new CesSubmissionListData();

        // when
        throttlingAdapter.activateLockIfRequired(data);

        // then
        assertEquals(0, throttlingAdapter.getSubmissionIpMap().size());
    }

    @Test
    public void test_activateLockIfRequired_maxNumberExceeded() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 30, 60);
        CesSubmissionListData data = new CesSubmissionListData();
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, data);
        throttlingAdapter.recordSubmissionFailure(null);

        // when
        throttlingAdapter.activateLockIfRequired(data);

        // then
        assertNotNull(throttlingAdapter.getSubmissionIpMap().get(CLIENT_ID));
        assertNotNull(throttlingAdapter.getSubmissionIpMap().get(CLIENT_ID).getLockTime());
    }

    @Test
    public void test_activateLockIfRequired_maxNumberNotExceeded() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(2, 30, 60);
        CesSubmissionListData data = new CesSubmissionListData();
        throttlingAdapter.getSubmissionIpMap().put(CLIENT_ID, data);
        throttlingAdapter.recordSubmissionFailure(null);

        // when
        throttlingAdapter.activateLockIfRequired(data);

        // then
        assertNotNull(throttlingAdapter.getSubmissionIpMap().get(CLIENT_ID));
        assertNull(throttlingAdapter.getSubmissionIpMap().get(CLIENT_ID).getLockTime());
    }

    @Test
    public void test_getHostIdentifier() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(2, 30, 60);

        // when
        String hostIdentifier = throttlingAdapter.getHostIdentifier();

        // then
        assertEquals(CLIENT_ID, hostIdentifier);
    }

    @Test
    public void test_isHostLocked_locked() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 60, 60);

        // when
        throttlingAdapter.recordSubmissionFailure(null);
        throttlingAdapter.recordSubmissionFailure(null);

        // then
        assertTrue(throttlingAdapter.isHostLocked());
    }

    @Test
    public void test_isHostLocked_notLocked() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(10, 60, 60);

        // when
        throttlingAdapter.recordSubmissionFailure(null);

        // then
        assertFalse(throttlingAdapter.isHostLocked());
    }

    @Test
    public void test_isHostLocked_invalidatedLock() {
        // given
        CesThrottlingInterceptorAdapter throttlingAdapter = Setup(1, 60, 0);

        // when
        throttlingAdapter.recordSubmissionFailure(null);
        throttlingAdapter.recordSubmissionFailure(null);

        // then
        assertFalse(throttlingAdapter.isHostLocked());
    }

}