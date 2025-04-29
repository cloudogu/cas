package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.services.ServiceRegistryProperties;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.File;

import org.apereo.cas.services.*;
import java.nio.file.Path;
import java.util.*;

@Configuration("CasCustomTemplateManagerConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.services")
@Slf4j
public class CasCustomTemplateManagerConfiguration {


    //Fixes: Parameter 1 of method cesDebugServiceRegistry in de.triology.cas.services.CasCustomTemplateManagerConfiguration required a bean of type 'org.apereo.cas.services.util.RegisteredServiceJsonSerializer' that could not be found.
    @Bean
    @RefreshScope
    public RegisteredServiceJsonSerializer registeredServiceJsonSerializer(
            final ConfigurableApplicationContext applicationContext
    ) {
        return new CesRegisteredServiceJsonSerializer(applicationContext);
    }

    @Bean
    @RefreshScope
    public RegisteredServicesTemplatesManager registeredServicesTemplatesManager(
            final CasConfigurationProperties casProperties,
            final RegisteredServiceJsonSerializer registeredServiceJsonSerializer
    ) {
        LOGGER.info("Overriding default RegisteredServicesTemplatesManager with CesLegacyCompatibleTemplatesManager");

        ServiceRegistryProperties serviceRegistryProperties = casProperties.getServiceRegistry();

        Collection<File> serviceTemplateResources;
        try {
            File directory = serviceRegistryProperties.getTemplates().getDirectory().getLocation().getFile();
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            serviceTemplateResources = files != null ? Arrays.asList(files) : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.debug("Could not load template directory: {}", e.getMessage(), e);
            serviceTemplateResources = Collections.emptyList();
        }

        return new CesLegacyCompatibleTemplatesManager(serviceTemplateResources, registeredServiceJsonSerializer);
    }

    @Bean
    @RefreshScope
    public ServiceRegistry cesDebugServiceRegistry(
            final CasConfigurationProperties casProperties,
            final RegisteredServiceJsonSerializer serializer,
            final ConfigurableApplicationContext applicationContext,
            final ApplicationEventPublisher eventPublisher
    ) {
        try {
            Path location = casProperties.getServiceRegistry().getJson().getLocation().getFile().toPath();
            LOGGER.debug("Using CesDebugServiceRegistry from path: {}", location);
            return new CesAbstractResourceBasedServiceRegistry(
                    location,
                    serializer,
                    applicationContext,
                    List.of()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CesDebugServiceRegistry", e);
        }
    }

    @Bean
    @RefreshScope
    public ServiceRegistryExecutionPlanConfigurer cesDebugServiceRegistryExecutionPlanConfigurer(
            final ServiceRegistry cesDebugServiceRegistry
    ) {
        return plan -> {
            LOGGER.debug("Registering CesDebugServiceRegistry in ServiceRegistryExecutionPlan");
            plan.registerServiceRegistry(cesDebugServiceRegistry);
        };
    }
}
