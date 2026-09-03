package de.triology.cas.pat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class PATSecurityHandlersTest {

    @Test
    void writesStableJsonAuthenticationError() throws Exception {
        Instant now = Instant.parse("2026-08-27T12:00:00Z");
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        PATSecurityHandlers handlers = new PATSecurityHandlers(
                objectMapper, Clock.fixed(now, ZoneOffset.UTC));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handlers.commence(new MockHttpServletRequest(), response, new BadCredentialsException("secret detail"));

        assertEquals(401, response.getStatus());
        assertEquals("Basic realm=\"PAT API\"", response.getHeader("WWW-Authenticate"));
        assertEquals("application/json", response.getContentType());
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("UNAUTHORIZED", body.get("code").asText());
        assertEquals("Authentication is required", body.get("message").asText());
        assertEquals(now, objectMapper.treeToValue(body.get("timestamp"), Instant.class));
    }
}
