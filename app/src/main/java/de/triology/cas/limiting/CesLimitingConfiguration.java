package de.triology.cas.limiting;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.throttle.ThrottledRequestResponseHandler;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;

@Configuration("CesLimitingConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CesLimitingConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(CesLimitingConfiguration.class);

    @Value("${cas.authn.throttle.failure.max_number:0}")
    private long max_number;

    @Value("${cas.authn.throttle.failure.failure_store_time:0}")
    private long failure_store_time;

    @Value("${cas.authn.throttle.failure.lockTime:0}")
    private long lockTime;

    @Bean
    @RefreshScope
    public ConcurrentHashMap<String, CesSubmissionListData> createCustomThrottleSubmissionMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @RefreshScope
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ThrottledSubmissionHandlerInterceptor authenticationThrottle(ConcurrentHashMap<String, CesSubmissionListData> createCustomThrottleSubmissionMap,
                                                                        ThrottledRequestResponseHandler throttledRequestResponseHandler) {
        if (max_number <= 0 && failure_store_time <= 0) {
            LOG.debug("Authentication throttling is disabled since no max_number and no failure_store_time is defined");
            return ThrottledSubmissionHandlerInterceptor.noOp();
        }

        LOG.debug("Activating CES authentication throttling based on IP address. Configuration[MaxNumber:[{}], FailureStoreTime:[{}], LockTime:[{}]]", max_number, failure_store_time, lockTime);
        return new CesThrottlingInterceptorAdapter(
                createCustomThrottleSubmissionMap,
                throttledRequestResponseHandler,
                CesThrottlingInterceptorAdapter.IClientInfoProvider.clientInfoHolder(),
                max_number,
                failure_store_time,
                lockTime);
    }
}