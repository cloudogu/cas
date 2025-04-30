package de.triology.cas.services;

import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesRegisteredServiceJsonSerializer}.
 */
class CesRegisteredServiceJsonSerializerTests {

    private CesRegisteredServiceJsonSerializer serializer;

    @BeforeEach
    void setUp() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        serializer = new CesRegisteredServiceJsonSerializer(context);
    }

    @Test
    void shouldCreateInstanceSuccessfully() {
        assertNotNull(serializer, "Serializer should be instantiated successfully");
    }

    @Test
    void shouldExtendRegisteredServiceJsonSerializer() {
        assertTrue(serializer instanceof RegisteredServiceJsonSerializer,
                "Serializer should extend RegisteredServiceJsonSerializer");
    }

    @Test
    void shouldReturnEmptyListWhenReadingInvalidJson() {
        // Simulate reading invalid JSON
        String invalidJson = "{ invalid json }";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
    
        var result = serializer.load(inputStream);
    
        assertTrue(result == null || result.isEmpty(), "Loading invalid JSON should return empty or null");
    }
}
