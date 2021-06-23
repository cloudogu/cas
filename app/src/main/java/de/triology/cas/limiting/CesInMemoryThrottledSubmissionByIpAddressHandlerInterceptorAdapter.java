package de.triology.cas.limiting;

import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.util.DateTimeUtils;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerConfigurationContext;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.apereo.inspektr.audit.AuditActionContext;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Throttles access attempts for failed logins by IP Address. This stores the attempts in memory.
 * Has the ability to remove locked IPs after a certain period of time.
 */
@Getter
public class CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapter
        extends HandlerInterceptorAdapter
        implements ThrottledSubmissionHandlerInterceptor, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapter.class);

    /**
     * Throttled login attempt action code used to tag the attempt in audit records.
     */
    public static final String ACTION_THROTTLED_LOGIN_ATTEMPT = "THROTTLED_LOGIN_ATTEMPT";

    /**
     * Number of milli-seconds in a second.
     */
    private static final double NUMBER_OF_MILLISECONDS_IN_SECOND = 1000.0;
    private double thresholdRate = -1;
    private static final double SUBMISSION_RATE_DIVIDEND = 1000.0;
    private final ThrottledSubmissionHandlerConfigurationContext configurationContext;
    private final ConcurrentMap<String, ZonedDateTime> submissionIpMap;
    private final ConcurrentMap<String, ZonedDateTime> lockIpMap;
    private final long lockTime;

    protected CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapter(
            final ThrottledSubmissionHandlerConfigurationContext configurationContext,
            final ConcurrentMap<String, ZonedDateTime> submissionIpMap,
            final long lockTime) {
        this.submissionIpMap = submissionIpMap;
        this.configurationContext = configurationContext;
        this.lockTime = lockTime;
        this.lockIpMap = new ConcurrentHashMap<>();

        LOGGER.debug("failureThreshold: [{}]", configurationContext.getFailureThreshold());
        LOGGER.debug("failureRangeInSeconds: [{}]", configurationContext.getFailureRangeInSeconds());
    }

    @Override
    public void afterPropertiesSet() {
        this.thresholdRate = (double) configurationContext.getFailureThreshold() / configurationContext.getFailureRangeInSeconds();
        LOGGER.debug("Calculated threshold rate as [{}]", this.thresholdRate);
    }

    public String constructKey(final HttpServletRequest request) {
        return ClientInfoHolder.getClientInfo().getClientIpAddress();
    }

    @Override
    public final boolean preHandle(final HttpServletRequest request,
                                   final HttpServletResponse response, final Object o) throws Exception {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            LOGGER.trace("Letting the request through given http method is [{}]", request.getMethod());
            return true;
        }

        boolean locked = isRequestTimeLocked(request);
        if (locked) {
            LOGGER.warn("Throttling submission from [{}]. The host is still locked and cannot send any requests.", request.getRemoteAddr());
            return configurationContext.getThrottledRequestResponseHandler().handle(request, response);
        }

        val throttled = throttleRequest(request, response) || exceedsThreshold(request);
        if (throttled) {
            LOGGER.warn("Throttling submission from [{}]. More than [{}] failed login attempts within [{}] seconds. "
                            + "Authentication attempt exceeds the failure threshold [{}]", request.getRemoteAddr(),
                    this.thresholdRate, configurationContext.getFailureRangeInSeconds(), configurationContext.getFailureThreshold());

            // lock the host ip oft the request
            recordLockEntry(request);

            return configurationContext.getThrottledRequestResponseHandler().handle(request, response);
        }
        return true;
    }

    @Override
    public final void postHandle(final HttpServletRequest request, final HttpServletResponse response,
                                 final Object o, final ModelAndView modelAndView) {
        if (!HttpMethod.POST.name().equals(request.getMethod())) {
            LOGGER.trace("Skipping authentication throttling for requests other than POST");
            return;
        }
        val recordEvent = shouldResponseBeRecordedAsFailure(response);
        if (recordEvent) {
            LOGGER.debug("Recording submission failure for [{}]", request.getRequestURI());
            recordSubmissionFailure(request);
        } else {
            LOGGER.trace("Skipping to record submission failure for [{}] with response status [{}]",
                    request.getRequestURI(), response.getStatus());
        }
    }


    /**
     * Is request throttled.
     *
     * @param request  the request
     * @param response the response
     * @return true if the request is throttled. False otherwise, letting it proceed.
     */
    protected boolean throttleRequest(final HttpServletRequest request, final HttpServletResponse response) {
        return configurationContext.getThrottledRequestExecutor() != null
                && configurationContext.getThrottledRequestExecutor().throttle(request, response);
    }

    /**
     * Should response be recorded as failure boolean.
     *
     * @param response the response
     * @return true/false
     */
    protected boolean shouldResponseBeRecordedAsFailure(final HttpServletResponse response) {
        val status = response.getStatus();
        return status != HttpStatus.SC_CREATED && status != HttpStatus.SC_OK && status != HttpStatus.SC_MOVED_TEMPORARILY;
    }

    /**
     * Calculate threshold rate and compare boolean.
     * Compute rate in submissions/sec between last two authn failures and compare with threshold.
     *
     * @param failures the failures
     * @return true/false
     */
    @SuppressWarnings("JdkObsolete")
    protected boolean calculateFailureThresholdRateAndCompare(final List<Date> failures) {
        if (failures.size() < 2) {
            return false;
        }
        val lastTime = failures.get(0).getTime();
        val secondToLastTime = failures.get(1).getTime();
        val difference = lastTime - secondToLastTime;
        val rate = NUMBER_OF_MILLISECONDS_IN_SECOND / difference;
        LOGGER.debug("Last attempt was at [{}] and the one before that was at [{}]. Difference is [{}] calculated as rate of [{}]",
                lastTime, secondToLastTime, difference, rate);
        if (rate > getThresholdRate()) {
            LOGGER.warn("Authentication throttling rate [{}] exceeds the defined threshold [{}]", rate, getThresholdRate());
            return true;
        }
        return false;
    }

    /**
     * Construct username from the request.
     *
     * @param request the request
     * @return the string
     */
    protected String getUsernameParameterFromRequest(final HttpServletRequest request) {
        return request.getParameter(StringUtils.defaultString(configurationContext.getUsernameParameter(), "username"));
    }

    /**
     * Records an audit action.
     *
     * @param request    The current HTTP request.
     * @param actionName Name of the action to be recorded.
     */
    protected void recordAuditAction(final HttpServletRequest request, final String actionName) {
        val userToUse = getUsernameParameterFromRequest(request);
        val clientInfo = ClientInfoHolder.getClientInfo();
        val resource = StringUtils.defaultString(request.getParameter(CasProtocolConstants.PARAMETER_SERVICE), "N/A");
        val context = new AuditActionContext(
                userToUse,
                resource,
                actionName,
                configurationContext.getApplicationCode(),
                DateTimeUtils.dateOf(ZonedDateTime.now(ZoneOffset.UTC)),
                clientInfo.getClientIpAddress(),
                clientInfo.getServerIpAddress());
        LOGGER.debug("Recording throttled audit action [{}]", context);
        configurationContext.getAuditTrailExecutionPlan().record(context);
    }

    @Override
    public void decrement() {
        LOGGER.info("Beginning audit cleanup...");
        LOGGER.info("Before:" + submissionIpMap);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        this.submissionIpMap.entrySet().removeIf(entry -> !isTimeLocked(now, entry.getValue(), this.lockTime));
        LOGGER.info("After:" + submissionIpMap);
        LOGGER.debug("Done decrementing count for throttler.");
    }

    @Override
    public boolean exceedsThreshold(final HttpServletRequest request) {
        ZonedDateTime last = this.submissionIpMap.get(constructKey(request));
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return last != null && submissionRate(now, last) > getThresholdRate();
    }

    public boolean isRequestTimeLocked(final HttpServletRequest request) {
        ZonedDateTime last = this.lockIpMap.get(constructKey(request));
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return last != null && isTimeLocked(now, last, this.lockTime);
    }

    @Override
    public String getName() {
        return "inMemoryIpAddressLockTimeThrottle";
    }

    @Override
    public Collection getRecords() {
        return submissionIpMap.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "<->" + entry.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public void recordSubmissionFailure(final HttpServletRequest request) {
        val key = constructKey(request);
        LOGGER.debug("Recording submission failure [{}]", key);
        this.submissionIpMap.put(key, ZonedDateTime.now(ZoneOffset.UTC));
    }

    public void recordLockEntry(final HttpServletRequest request) {
        val key = constructKey(request);
        LOGGER.debug("Recording submission failure [{}]", key);
        this.lockIpMap.put(key, ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Gets failure in range cut off date.
     *
     * @return the failure in range cut off date
     */
    protected Date getFailureInRangeCutOffDate() {
        val cutoff = ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(configurationContext.getFailureRangeInSeconds());
        return DateTimeUtils.timestampOf(cutoff);
    }

    /**
     * Computes the instantaneous rate in between two given dates corresponding to two submissions.
     *
     * @param a First date.
     * @param b Second date.
     * @return Instantaneous submission rate in submissions/sec, e.g. {@code a - b}.
     */
    private static double submissionRate(final ZonedDateTime a, final ZonedDateTime b) {
        return SUBMISSION_RATE_DIVIDEND / (a.toInstant().toEpochMilli() - b.toInstant().toEpochMilli());
    }

    /**
     * Computes the difference between the two dates and determines whether the locked entry
     * is still blocked.
     *
     * @param now               the current date time
     * @param lockedTimeEntry   the date time of the locked entry
     * @param lockTimeThreshold the minimum amount of time required to unlock an entry
     * @return true when account is still locked
     */
    public static boolean isTimeLocked(final ZonedDateTime now, final ZonedDateTime lockedTimeEntry, long lockTimeThreshold) {
        double differenceInSeconds = (now.toInstant().toEpochMilli() - lockedTimeEntry.toInstant().toEpochMilli()) / 1000d;
        LOGGER.debug("Remove-Check [Diff in Sekunden:{}, LockTime:{}] = [{}] ", differenceInSeconds, lockTimeThreshold, (differenceInSeconds < lockTimeThreshold));
        return differenceInSeconds < lockTimeThreshold;
    }
}
