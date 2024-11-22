package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("CesServicesSpringConfiguration")
@ComponentScan("de.triology.cas.services")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesServicesSpringConfiguration {

    @Bean(name = "serviceMatchingStrategy")
    public ServiceMatchingStrategy serviceMatchingStrategy() {
        return new CesServiceMatchingStrategy();
    }
}
