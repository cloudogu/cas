package de.triology.cas.services;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.ServicesManagerExecutionPlanConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration("CesServicesConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.services")
public class CesServicesConfiguration implements ServicesManagerExecutionPlanConfigurer {

    @Autowired
    private RegistryEtcd registry;

    @Value("${ces.services.stage:production}")
    private String stage;

    @Value("${ces.services.allowedAttributes}")
    private List<String> allowedAttributes;

    @Override
    public ServicesManager configureServicesManager() {
        return new CesServicesManager(allowedAttributes, stage, registry);
    }
}