package de.triology.cas.limiting;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.apereo.cas.throttle.AuthenticationThrottlingExecutionPlan;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * This is {@link InMemoryThrottledSubmissionLockTimeCleaner}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@RequiredArgsConstructor
public class InMemoryThrottledSubmissionLockTimeCleaner implements Runnable {
    private final AuthenticationThrottlingExecutionPlan authenticationThrottlingExecutionPlan;

    /**
     * Kicks off the job that attempts to clean the throttling submission record history.
     * TODO: implement a lock time based cleanup strategy
     */
    @Override
    @Scheduled(initialDelayString = "${cas.authn.throttle.schedule.start-delay:PT10S}",
        fixedDelayString = "${cas.authn.throttle.schedule.repeat-interval:PT15S}")
    public void run() {
        // old implementation
        val handlers = authenticationThrottlingExecutionPlan.getAuthenticationThrottleInterceptors();
        handlers
            .stream()
            .filter(handler -> handler instanceof ThrottledSubmissionHandlerInterceptor)
            .map(handler -> (ThrottledSubmissionHandlerInterceptor) handler)
            .forEach(ThrottledSubmissionHandlerInterceptor::decrement);
    }
}
