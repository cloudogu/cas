package de.triology.cas.services;

import org.apereo.cas.services.RegisteredService;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link CesServicesManagerStageDevelopment}.
 */
public class CesServicesManagerStageDevelopmentTest {
    /**
     * ID of the service used in development mode.
     */
    private static final long DEVELOPMENT_SERVICE_ID = 1;
    /**
     * Service ID of the service used in development mode.
     */
    private static final String DEVELOPMENT_SERVICE_SERVICE_ID = "^(https?|imaps?)://.*";

    CesServicesManagerStageDevelopment stage = new CesServicesManagerStageDevelopment(new CesServiceManagerConfiguration("development", null, null, false, null, null));

    /**
     * Test for {@link CesServicesManagerStageDevelopment#getRegisteredServices()}
     */
    @Test
    public void getRegisteredServices() {
        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();
        assertEquals("Unexpected amount of services returned in development mode", 1, allServices.size());
        assertEquals("Development service not returned by getRegisteredServices(). Id mismatch.",
                DEVELOPMENT_SERVICE_ID, allServices.iterator().next().getId());
        assertEquals("Development service not returned by getRegisteredServices(). ServiceId mismatch.",
                DEVELOPMENT_SERVICE_SERVICE_ID, allServices.iterator().next().getServiceId());
    }
}