package de.triology.cas.pat.controller;

import java.security.Principal;
import java.time.Clock;

import de.triology.cas.pat.model.PATErrorResponse;
import de.triology.cas.pat.service.PATNotFoundException;
import de.triology.cas.pat.service.PATRequestException;
import de.triology.cas.pat.service.PATStorageUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converts PAT controller failures to the stable, secret-safe JSON error contract.
 */
@RestControllerAdvice(assignableTypes = PATController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PATExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PATExceptionHandler.class);
    private static final Logger AUDIT = LoggerFactory.getLogger("de.triology.cas.pat.audit");

    private final Clock clock;

    /**
     * Creates the handler with the PAT clock.
     *
     * @param clock clock used for error timestamps
     */
    public PATExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Maps domain validation failures to HTTP 400.
     *
     * @param exception validation failure
     * @param principal authenticated caller, when available
     * @return structured bad-request response
     */
    @ExceptionHandler(PATRequestException.class)
    public ResponseEntity<PATErrorResponse> invalidRequest(PATRequestException exception, Principal principal) {
        AUDIT.warn("event=pat_request principal={} result=invalid_request", principalName(principal));
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    /**
     * Maps malformed JSON, binding failures and invalid path types to HTTP 400.
     *
     * @param exception binding or conversion failure
     * @param principal authenticated caller, when available
     * @return structured bad-request response
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<PATErrorResponse> malformedRequest(Exception exception, Principal principal) {
        AUDIT.warn("event=pat_request principal={} result=invalid_request", principalName(principal));
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", invalidRequestMessage(exception));
    }

    /**
     * Selects a stable, client-safe message for request binding failures.
     *
     * @param exception request binding failure
     * @return message suitable for the public error response
     */
    private String invalidRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            return validationException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .filter(message -> message != null && !message.isBlank())
                    .findFirst()
                    .orElse("Request is invalid");
        }
        if (exception instanceof MethodArgumentTypeMismatchException typeMismatchException
                && "id".equals(typeMismatchException.getName())) {
            return "Invalid PAT id";
        }
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof IllegalArgumentException
                    && cause.getMessage() != null
                    && cause.getMessage().startsWith("Unknown field: ")) {
                return cause.getMessage();
            }
            cause = cause.getCause();
        }
        return "Malformed JSON request";
    }

    /**
     * Maps missing or owner-mismatched PATs to HTTP 404.
     *
     * @param exception not-found failure
     * @return structured not-found response
     */
    @ExceptionHandler(PATNotFoundException.class)
    public ResponseEntity<PATErrorResponse> notFound(PATNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "PAT_NOT_FOUND", exception.getMessage());
    }

    /**
     * Maps database availability failures to HTTP 503 without exposing internals.
     *
     * @param exception persistence failure retained only for server logs
     * @return structured service-unavailable response
     */
    @ExceptionHandler(PATStorageUnavailableException.class)
    public ResponseEntity<PATErrorResponse> storageUnavailable(Exception exception) {
        LOGGER.error("PAT storage operation failed", exception);
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "PAT storage is unavailable");
    }

    /**
     * Handles unexpected PAT failures as secret-safe HTTP 500 responses.
     *
     * @param exception unexpected internal failure
     * @return structured internal-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<PATErrorResponse> internalError(Exception exception) {
        LOGGER.error("Unexpected PAT service error", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected internal error occurred");
    }

    /**
     * Builds a timestamped error response with the requested HTTP status.
     *
     * @param status HTTP response status
     * @param code stable machine-readable code
     * @param message client-safe message
     * @return complete response entity
     */
    private ResponseEntity<PATErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(PATErrorResponse.of(code, message, clock.instant()));
    }

    /**
     * Returns a safe principal label for audit logging.
     *
     * @param principal current principal, possibly {@code null}
     * @return principal name or {@code anonymous}
     */
    private String principalName(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }
}
