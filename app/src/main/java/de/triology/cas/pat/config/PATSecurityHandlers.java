package de.triology.cas.pat.config;

import java.io.IOException;
import java.time.Clock;

import tools.jackson.databind.ObjectMapper;
import de.triology.cas.pat.model.PATErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Produces the PAT API JSON error format for Spring Security authentication failures.
 */
public class PATSecurityHandlers implements AuthenticationEntryPoint {
    private static final Logger AUDIT = LoggerFactory.getLogger("de.triology.cas.pat.audit");
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Creates security handlers using the application JSON mapper.
     *
     * @param objectMapper mapper used to serialize error responses
     * @param clock clock used for error timestamps
     */
    public PATSecurityHandlers(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Handles unauthenticated PAT requests as HTTP 401 responses.
     *
     * @param request rejected request
     * @param response response to populate
     * @param authException authentication failure
     * @throws IOException when the JSON response cannot be written
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        AUDIT.warn("event=pat_access result=unauthorized");
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"PAT API\"");
        write(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required");
    }

    /**
     * Serializes a security error directly to the servlet response.
     *
     * @param response response to populate
     * @param status HTTP status code
     * @param code stable API error code
     * @param message client-safe message
     * @throws IOException when serialization fails
     */
    private void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), PATErrorResponse.of(code, message, clock.instant()));
    }
}
