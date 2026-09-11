package de.triology.cas.pat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class PATMethodNotAllowedHandlerTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private final PATMethodNotAllowedHandler handler =
            new PATMethodNotAllowedHandler(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsPatErrorAndAllowHeaderForPatRoutes() throws Exception {
        var exception = new HttpRequestMethodNotSupportedException("PUT", Set.of("GET", "DELETE"));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/cas/api/users/user/pats/id");
        request.setContextPath("/cas");

        var response = handler.methodNotAllowed(exception, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.DELETE), response.getHeaders().getAllow());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().code());
        assertEquals("PATs cannot be updated", response.getBody().message());
        assertEquals(NOW, response.getBody().timestamp());
    }

    @Test
    void omitsAllowHeaderWhenFrameworkProvidesNoSupportedMethods() throws Exception {
        var exception = new HttpRequestMethodNotSupportedException("PATCH");
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/users/user/pats");

        var response = handler.methodNotAllowed(exception, request);

        assertEquals(Set.of(), response.getHeaders().getAllow());
    }

    @Test
    void rethrowsForUnrelatedRoutes() {
        var exception = new HttpRequestMethodNotSupportedException("PATCH");
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/unrelated");

        var thrown = assertThrows(HttpRequestMethodNotSupportedException.class,
                () -> handler.methodNotAllowed(exception, request));

        assertSame(exception, thrown);
    }
}
