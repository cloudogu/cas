package de.triology.cas.limiting;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManagerExecutionPlanConfigurer;
import org.apereo.cas.throttle.AuthenticationThrottlingExecutionPlan;
import org.apereo.cas.web.support.InMemoryThrottledSubmissionCleaner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("CesLimitingConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.limiting")
public class CesLimitingConfiguration {

    //TODO: autowire lock time

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public Runnable throttleSubmissionCleaner(final AuthenticationThrottlingExecutionPlan plan) {
        return new InMemoryThrottledSubmissionLockTimeCleaner(plan);
    }
}