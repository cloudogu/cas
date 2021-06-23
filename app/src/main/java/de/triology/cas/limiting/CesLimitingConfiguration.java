package de.triology.cas.limiting;

import lombok.val;
import org.apereo.cas.audit.AuditTrailExecutionPlan;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.throttle.ThrottledRequestExecutor;
import org.apereo.cas.throttle.ThrottledRequestResponseHandler;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerConfigurationContext;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentMap;

@Configuration("CesLimitingConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.limiting")
public class CesLimitingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CesLimitingConfiguration.class);

    @Value("${cas.authn.throttle.failure.lockTime:0}")
    private long lockTime;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    @RefreshScope
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ThrottledSubmissionHandlerInterceptor authenticationThrottle(ConcurrentMap throttleSubmissionMap,
                                                                        ThrottledRequestExecutor throttledRequestExecutor,
                                                                        ThrottledRequestResponseHandler throttledRequestResponseHandler,
                                                                        AuditTrailExecutionPlan auditTrailExecutionPlan) {
        val throttle = casProperties.getAuthn().getThrottle();

        if (throttle.getFailure().getRangeSeconds() <= 0 && throttle.getFailure().getThreshold() <= 0) {
            LOGGER.debug("Authentication throttling is disabled since no range-seconds or failure-threshold is defined");
            return ThrottledSubmissionHandlerInterceptor.noOp();
        }

        val context = ThrottledSubmissionHandlerConfigurationContext.builder()
                .failureThreshold(throttle.getFailure().getThreshold())
                .failureRangeInSeconds(throttle.getFailure().getRangeSeconds())
                .usernameParameter(throttle.getUsernameParameter())
                .authenticationFailureCode(throttle.getFailure().getCode())
                .auditTrailExecutionPlan(auditTrailExecutionPlan)
                .applicationCode(throttle.getAppCode())
                .throttledRequestResponseHandler(throttledRequestResponseHandler)
                .throttledRequestExecutor(throttledRequestExecutor)
                .build();

        LOGGER.debug("Activating CES authentication throttling based on IP address...");
        return new CesInMemoryThrottledSubmissionByIpAddressHandlerInterceptorAdapter(context, throttleSubmissionMap, lockTime);
    }
}