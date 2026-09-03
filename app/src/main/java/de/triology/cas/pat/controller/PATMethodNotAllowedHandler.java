package de.triology.cas.pat.controller;

import java.time.Clock;
import java.util.Set;
import java.util.regex.Pattern;

import de.triology.cas.pat.model.PATErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts unsupported methods on PAT routes to the PAT error contract while retaining the Allow header.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PATMethodNotAllowedHandler {
    private static final Pattern PAT_PATH = Pattern.compile("^/api/users/[^/]+/pats(?:/.*)?$");

    private final Clock clock;

    /**
     * Creates the handler with the PAT clock.
     *
     * @param clock clock used for error timestamps
     */
    public PATMethodNotAllowedHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Handles unsupported methods only for PAT API paths.
     *
     * @param exception method mismatch reported by Spring MVC
     * @param request current request
     * @return PAT-specific method-not-allowed response
     * @throws HttpRequestMethodNotSupportedException for non-PAT paths
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<PATErrorResponse> methodNotAllowed(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) throws HttpRequestMethodNotSupportedException {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if (!PAT_PATH.matcher(requestPath).matches()) {
            throw exception;
        }

        HttpHeaders headers = new HttpHeaders();
        Set<HttpMethod> supportedMethods = exception.getSupportedHttpMethods();
        if (supportedMethods != null) {
            headers.setAllow(supportedMethods);
        }
        PATErrorResponse body = PATErrorResponse.of(
                "METHOD_NOT_ALLOWED",
                "PATs cannot be updated",
                clock.instant());
        return new ResponseEntity<>(body, headers, HttpStatus.METHOD_NOT_ALLOWED);
    }
}
