package de.triology.cas.limiting;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.throttle.ThrottledRequestResponseHandler;
import org.apereo.cas.web.support.ThrottledSubmissionHandlerInterceptor;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration("CesLimitingConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.limiting")
public class CesLimitingConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(CesLimitingConfiguration.class);

    @Value("${cas.authn.throttle.failure.max_number:0}")
    private long max_number;
    @Value("${cas.authn.throttle.failure.failure_store_time:0}")
    private long failure_store_time;
    @Value("${cas.authn.throttle.failure.lockTime:0}")
    private long lockTime;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Bean
    @RefreshScope
    public ConcurrentMap throttleSubmissionMap() {
        return new ConcurrentHashMap<String, CesSubmissionListData>();
    }

    @Bean
    @RefreshScope
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ThrottledSubmissionHandlerInterceptor authenticationThrottle(ConcurrentMap throttleSubmissionMap,
                                                                        ThrottledRequestResponseHandler throttledRequestResponseHandler) {

        if (max_number <= 0 && failure_store_time <= 0) {
            LOGGER.debug("Authentication throttling is disabled since no max_number or failure_store_time is defined");
            return ThrottledSubmissionHandlerInterceptor.noOp();
        }

        LOGGER.debug("Activating CES authentication throttling based on IP address. Configuration[MaxNumber:[{}], FailureStoreTime:[{}], LockTime:[{}]]", max_number, failure_store_time, lockTime);
        return new CesThrottlingInterceptorAdapter(
                throttleSubmissionMap,
                throttledRequestResponseHandler,
                CesThrottlingInterceptorAdapter.IClientInfoProvider.clientInfoHolder(),
                max_number,
                failure_store_time,
                lockTime);
    }
}