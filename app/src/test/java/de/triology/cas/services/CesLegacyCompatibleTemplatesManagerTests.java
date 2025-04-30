package de.triology.cas.services;

import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.util.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesLegacyCompatibleTemplatesManager}.
 */
class CesLegacyCompatibleTemplatesManagerTests {

    private CesLegacyCompatibleTemplatesManager templatesManager;

    @BeforeEach
    void setUp() {
        // Mock serializer since we don't actually serialize in these tests
        @SuppressWarnings("unchecked")
        StringSerializer<RegisteredService> serializer = mock(StringSerializer.class);

        templatesManager = new CesLegacyCompatibleTemplatesManager(Collections.emptyList(), serializer);
    }

    @Test
    void apply_ShouldReturnOriginalService_WhenNotCasRegisteredService() {
        // given
        RegisteredService service = mock(RegisteredService.class);

        // when
        RegisteredService result = templatesManager.apply(service);

        // then
        assertSame(service, result, "Service should be returned as-is if not a CasRegisteredService");
    }

    @Test
    void apply_ShouldFallbackName_WhenNameIsMissing() {
        // given
        CasRegisteredService service = new CasRegisteredService();
        service.setName(null); // missing name
        service.setProperties(Map.of(
                "ServiceName", createPropertyWithValue("fallback-name")
        ));

        // when
        CasRegisteredService result = (CasRegisteredService) templatesManager.apply(service);

        // then
        assertEquals("fallback-name", result.getName(), "Name should be set from ServiceName property");
    }

    @Test
    void apply_ShouldFallbackServiceId_WhenServiceIdIsMissing() {
        // given
        CasRegisteredService service = new CasRegisteredService();
        service.setServiceId(null); // missing serviceId
        service.setProperties(Map.of(
                "Fqdn", createPropertyWithValue("example.org"),
                "ServiceName", createPropertyWithValue("myapp")
        ));

        // when
        CasRegisteredService result = (CasRegisteredService) templatesManager.apply(service);

        // then
        assertEquals("^https://example.org/myapp(/.*)?$", result.getServiceId(), "ServiceId should be constructed from Fqdn and ServiceName");
    }

    @Test
    void apply_ShouldNotChangeNameOrServiceId_WhenAlreadyPresent() {
        // given
        CasRegisteredService service = new CasRegisteredService();
        service.setName("ExistingName");
        service.setServiceId("ExistingServiceId");
        service.setProperties(Collections.emptyMap());

        // when
        CasRegisteredService result = (CasRegisteredService) templatesManager.apply(service);

        // then
        assertEquals("ExistingName", result.getName(), "Existing name should not be changed");
        assertEquals("ExistingServiceId", result.getServiceId(), "Existing serviceId should not be changed");
    }

    @Test
    void apply_ShouldHandleMissingPropertiesGracefully() {
        // given
        CasRegisteredService service = new CasRegisteredService();
        service.setProperties(Collections.emptyMap()); // no props at all

        // when
        CasRegisteredService result = (CasRegisteredService) templatesManager.apply(service);

        // then
        assertNotNull(result, "Service should still be returned without crashing even if properties are missing");
    }

    // Helper to create a RegisteredServiceProperty with a single value
    private RegisteredServiceProperty createPropertyWithValue(String value) {
        RegisteredServiceProperty prop = mock(RegisteredServiceProperty.class);
        when(prop.getValues()).thenReturn(Set.of(value));
        return prop;
    }
}
