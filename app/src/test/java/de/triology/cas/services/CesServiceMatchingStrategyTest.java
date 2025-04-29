package de.triology.cas.services;

import org.apereo.cas.authentication.principal.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesServiceMatchingStrategy}.
 */
class CesServiceMatchingStrategyTests {

    private CesServiceMatchingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CesServiceMatchingStrategy();
    }

    @Test
    void shouldMatchSameServiceIgnoringPortAndFragment() {
        Service service = mock(Service.class);
        Service serviceToMatch = mock(Service.class);

        when(service.getId()).thenReturn("https://example.org:8443/app#section");
        when(serviceToMatch.getId()).thenReturn("https://example.org/app");

        boolean result = strategy.matches(service, serviceToMatch);
        assertTrue(result, "Services should match ignoring port and hash fragment");
    }

    @Test
    void shouldNotMatchDifferentServices() {
        Service service = mock(Service.class);
        Service serviceToMatch = mock(Service.class);

        when(service.getId()).thenReturn("https://example.org/app");
        when(serviceToMatch.getId()).thenReturn("https://different.org/app");

        boolean result = strategy.matches(service, serviceToMatch);
        assertFalse(result, "Different services should not match");
    }

    @Test
    void shouldReturnFalseIfEitherServiceIsNull() {
        Service service = mock(Service.class);

        boolean result1 = strategy.matches(service, null);
        boolean result2 = strategy.matches(null, service);

        assertFalse(result1, "Should return false if serviceToMatch is null");
        assertFalse(result2, "Should return false if service is null");
    }

    @Test
    void shouldDecodeUrlsBeforeComparing() {
        Service service = mock(Service.class);
        Service serviceToMatch = mock(Service.class);

        when(service.getId()).thenReturn("https://example.org/app%20with%20space");
        when(serviceToMatch.getId()).thenReturn("https://example.org/app with space");

        boolean result = strategy.matches(service, serviceToMatch);
        assertTrue(result, "Should decode URLs and match them");
    }

    @Test
    void shouldHandleMalformedUrlGracefully() {
        Service service = mock(Service.class);
        Service serviceToMatch = mock(Service.class);

        when(service.getId()).thenThrow(new RuntimeException("Simulated error"));
        when(serviceToMatch.getId()).thenReturn("https://example.org/app");

        boolean result = strategy.matches(service, serviceToMatch);
        assertFalse(result, "Should return false if decoding fails");
    }
}
