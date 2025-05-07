package de.triology.cas.services;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.services.ServiceRegistryProperties;
import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.apereo.cas.configuration.model.support.services.json.JsonServiceRegistryProperties;

class CasCustomTemplateManagerConfigurationTests {

    private CasCustomTemplateManagerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CasCustomTemplateManagerConfiguration();
    }

    @Test
    void registeredServiceJsonSerializer_ShouldReturnInstance() {
        var applicationContext = mock(ConfigurableApplicationContext.class);

        var serializer = configuration.registeredServiceJsonSerializer(applicationContext);

        assertNotNull(serializer);
        assertTrue(serializer instanceof CesRegisteredServiceJsonSerializer);
    }

    @Test
    void registeredServicesTemplatesManager_ShouldHandleExceptionGracefully() {
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getTemplates()).thenThrow(new RuntimeException("Test Exception"));

        var serializer = mock(RegisteredServiceJsonSerializer.class);

        var manager = configuration.registeredServicesTemplatesManager(casProperties, serializer);

        assertNotNull(manager);
        assertTrue(manager instanceof CesLegacyCompatibleTemplatesManager);
    }

    @Test
    void cesDebugServiceRegistry_ShouldReturnInstance() throws Exception {
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        var jsonProps = mock(JsonServiceRegistryProperties.class);

        // Create real temp directory instead of file
        File tempDir = Files.createTempDirectory("cas").toFile();
        var resource = mock(org.springframework.core.io.Resource.class);
        when(resource.getFile()).thenReturn(tempDir);

        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getJson()).thenReturn(jsonProps);
        when(jsonProps.getLocation()).thenReturn(resource);

        var serializer = mock(RegisteredServiceJsonSerializer.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var eventPublisher = mock(ApplicationEventPublisher.class);

        var registry = configuration.cesDebugServiceRegistry(casProperties, serializer, applicationContext, eventPublisher);

        assertNotNull(registry);
        assertTrue(registry instanceof CesAbstractResourceBasedServiceRegistry);

        tempDir.delete();
    }

    @Test
    void cesDebugServiceRegistryExecutionPlanConfigurer_ShouldReturnConfigurer() {
        var serviceRegistry = mock(ServiceRegistry.class);

        var configurer = configuration.cesDebugServiceRegistryExecutionPlanConfigurer(serviceRegistry);

        assertNotNull(configurer);
    }
}