package de.triology.cas.limiting;

import lombok.Getter;
import lombok.val;
import org.apache.http.HttpStatus;
import org.apereo.cas.throttle.ThrottledRequestResponseHandler;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Throttles access attempts for failed logins by IP Address. This stores the attempts in memory.
 * Has the ability to remove locked IPs after a certain period of time.
 */
@Getter
public class CesThrottlingInterceptorAdapter
        implements ThrottledSubmissionHandlerInterceptor, AsyncHandlerInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CesThrottlingInterceptorAdapter.class);

    /**
     * Throttled login attempt action code used to tag the attempt in audit records.
     */
    public static final String ACTION_THROTTLED_LOGIN_ATTEMPT = "THROTTLED_LOGIN_ATTEMPT";
    /**
     * Number of milli-seconds in a second.
     */
    private static final double NUMBER_OF_MILLISECONDS_IN_SECOND = 1000.0;

    private final ThrottledRequestResponseHandler throttledRequestResponseHandler;
    /**
     * Contains all failed authentication entries
     */
    private final ConcurrentMap<String, CesSubmissionListData> submissionIpMap;
    /**
     * Determines the maximum number of allowed failed submission the the range of failure_store_time
     */
    private final long max_number;
    /**
     * Determines the range of time (in seconds) that should only allow max_number failed requests
     */
    private final long failure_store_time;
    /**
     * Determines the time (in seconds) the host is locked after to many failed submissions
     */
    private final long lockTime;
    /**
     * Contains information about the current client. Required for testing.
     */
    private IClientInfoProvider clientInfoProvider;


    protected CesThrottlingInterceptorAdapter(
            final ConcurrentMap<String, CesSubmissionListData> submissionIpMap,
            final ThrottledRequestResponseHandler throttledRequestResponseHandler,
            final IClientInfoProvider clientInfoProvider,
            final long max_number,
            final long failure_store_time,
            final long lockTime) {
        this.submissionIpMap = submissionIpMap;
        this.throttledRequestResponseHandler = throttledRequestResponseHandler;
        this.clientInfoProvider = clientInfoProvider;
        this.max_number = max_number;
        this.failure_store_time = failure_store_time;
        this.lockTime = lockTime;
    }

    /**
     * This interface is used to retrieve the information that is used to distinct the actual user.
     * <p>
     * The {@link #clientInfoHolder()} provides a default provider that retrieves the IP address of the user.
     */
    interface IClientInfoProvider {
        /**
         * @return Returns the client identifier
         */
        default String getClientInfo() {
            return ClientInfoHolder.getClientInfo().getClientIpAddress();
        }

        /**
         * Default client info provider that retrieves the IP address of the user.
         *
         * @return the client info provider
         */
        static IClientInfoProvider clientInfoHolder() {
            return new IClientInfoProvider() {
            };
        }
    }

    @Override
    public final boolean preHandle(final HttpServletRequest request,
                                   final HttpServletResponse response, final Object o) throws Exception {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            LOG.trace("Letting the request through given http method is [{}]", request.getMethod());
            return true;
        }

        boolean throttled = isHostLocked();
        if (throttled) {
            LOG.warn("Throttling submission from [{}]. The host is locked and cannot send any requests. " +
                    "More than [{}] failed login attempts within [{}] seconds.", request.getRemoteAddr(), max_number, failure_store_time);
            return throttledRequestResponseHandler.handle(request, response);
        }
        return true;
    }

    @Override
    public final void postHandle(final HttpServletRequest request, final HttpServletResponse response,
                                 final Object o, final ModelAndView modelAndView) {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            LOG.trace("Skipping authentication throttling for requests other than POST");
            return;
        }

        val recordEvent = shouldResponseBeRecordedAsFailure(response);
        if (recordEvent) {
            LOG.debug("Recording submission failure for [{}]", request.getRequestURI());
            recordSubmissionFailure(request);
        } else {
            LOG.trace("Skipping to record submission failure for [{}] with response status [{}]",
                    request.getRequestURI(), response.getStatus());
        }
    }

    @Override
    public void recordSubmissionFailure(final HttpServletRequest request) {
        String hostIdentifier = getHostIdentifier();
        CesSubmissionListData hostSubmissionData = this.submissionIpMap.get(hostIdentifier);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        if (hostSubmissionData == null) {
            // create host submission data
            hostSubmissionData = new CesSubmissionListData();
            this.submissionIpMap.put(getHostIdentifier(), hostSubmissionData);
        }

        // invalidate the lock if it is no longer valid
        invalidateLockIfRequired(hostSubmissionData);

        // invalidate the submission data if max store time is exceeded
        invalidateSubmissionDataIfRequired(hostSubmissionData);

        // record failed submission
        LOG.debug("Recording submission failure [{}]", hostIdentifier);
        hostSubmissionData.recordFailedSubmission(now);

        // lock the host when necessary
        activateLockIfRequired(hostSubmissionData);
    }

    @Override
    public void release() {
        LOG.info("Beginning audit cleanup...");
        LOG.info("Before:" + submissionIpMap);
        this.submissionIpMap.entrySet().removeIf(entry -> {
            CesSubmissionListData data = entry.getValue();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            // remove entry when: lock is invalid
            ZonedDateTime timeAtLock = data.getLockTime();
            if (timeAtLock != null) {
                return getTimeDiffInSeconds(now, timeAtLock) >= lockTime;
            }

            // remove entry when: failure store time was exceeded
            ZonedDateTime firstSubmissionFailure = data.getFirstSubmissionFailure();
            if (firstSubmissionFailure != null) {
                return getTimeDiffInSeconds(now, firstSubmissionFailure) >= failure_store_time;
            } else {
                // remove empty entry
                return true;
            }
        });
        LOG.info("After:" + submissionIpMap);
        LOG.debug("Done decrementing count for throttler.");
    }

    @Override
    public String getName() {
        return "cesInMemoryIpAddressLockTimeThrottle";
    }

    @Override
    public Collection getRecords() {
        return submissionIpMap.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "<->" + entry.getValue())
                .collect(Collectors.toList());
    }

    /**
     * Determines whether a response should be recorded as failure.
     *
     * @param response the response
     * @return true, when the request should be recorded, otherwise false
     */
    boolean shouldResponseBeRecordedAsFailure(final HttpServletResponse response) {
        val status = response.getStatus();
        return status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK && status != HttpStatus.SC_MOVED_TEMPORARILY;
    }


    /**
     * Computes the difference between two given dates in seconds.
     *
     * @param now first time (newer)
     * @param old second time (older)
     * @return time difference in seconds
     */
    double getTimeDiffInSeconds(ZonedDateTime now, ZonedDateTime old) {
        return (now.toInstant().toEpochMilli() - old.toInstant().toEpochMilli()) / NUMBER_OF_MILLISECONDS_IN_SECOND;
    }

    /**
     * Checks whether the submission data is locked and if this lock is still valid.
     * The submission data is reset when a previous lock is no longer valid.
     *
     * @param hostSubmissionData The data of the submissions to check.
     * @return ture, when the host is still locked, otherwise false
     */
    boolean invalidateLockIfRequired(CesSubmissionListData hostSubmissionData) {
        ZonedDateTime timeAtLock = hostSubmissionData.getLockTime();
        if (timeAtLock == null) {
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        double differenceInSeconds = getTimeDiffInSeconds(now, timeAtLock);

        if (differenceInSeconds >= lockTime) {
            hostSubmissionData.reset();
            return false;
        }
        return true;
    }

    /**
     * Checks whether the submission failures are still valid. Resets the data when max failure store time was exceeded.
     *
     * @param hostSubmissionData The data of the submissions to check.
     */
    void invalidateSubmissionDataIfRequired(CesSubmissionListData hostSubmissionData) {
        ZonedDateTime firstFailure = hostSubmissionData.getFirstSubmissionFailure();
        if (firstFailure != null) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            double differenceInSeconds = getTimeDiffInSeconds(now, firstFailure);

            if (differenceInSeconds >= failure_store_time) {
                hostSubmissionData.reset();
            }
        }
    }

    /**
     * Checks whether the submission data is needs to be locked
     *
     * @param hostSubmissionData The data of the submissions to check.
     */
    void activateLockIfRequired(CesSubmissionListData hostSubmissionData) {
        if (hostSubmissionData.getLockTime() != null) {
            // already locked
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        if (hostSubmissionData.getFailedSubmissions() >= max_number) {
            // activate lock
            hostSubmissionData.recordHostLock(now);
        }
    }

    /**
     * Determines whether a host is currently locked
     *
     * @return true, when the host is currently locked
     */
    boolean isHostLocked() {
        CesSubmissionListData hostSubmissionData = this.submissionIpMap.get(getHostIdentifier());
        if (hostSubmissionData == null) {
            return false;
        }

        // invalidate the lock if it is no longer valid
        return invalidateLockIfRequired(hostSubmissionData);
    }

    /**
     * Identifies the current host.
     *
     * @return a distinctive identifier for the host
     */
    String getHostIdentifier() {
        return clientInfoProvider.getClientInfo();
    }
}
