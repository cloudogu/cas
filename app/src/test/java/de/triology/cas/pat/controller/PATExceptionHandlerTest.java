package de.triology.cas.pat.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import de.triology.cas.pat.model.PATErrorResponse;
import de.triology.cas.pat.service.PATNotFoundException;
import de.triology.cas.pat.service.PATRequestException;
import de.triology.cas.pat.service.PATStorageUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class PATExceptionHandlerTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private PATExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PATExceptionHandler(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void mapsDomainFailuresToStableResponses() {
        Principal principal = () -> "usermgt";
        assertResponse(handler.invalidRequest(new PATRequestException("invalid value"), principal),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "invalid value");
        assertResponse(handler.invalidRequest(new PATRequestException("invalid value"), null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "invalid value");
        assertResponse(handler.notFound(new PATNotFoundException()),
                HttpStatus.NOT_FOUND, "PAT_NOT_FOUND", "PAT not found");
        assertResponse(handler.storageUnavailable(new PATStorageUnavailableException(new RuntimeException("secret"))),
                HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "PAT storage is unavailable");
        assertResponse(handler.internalError(new RuntimeException("secret")),
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected internal error occurred");
    }

    @Test
    void mapsInvalidIdWithoutLeakingConversionDetails() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getName()).thenReturn("id");

        assertResponse(handler.malformedRequest(exception, null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid PAT id");
    }

    @Test
    void returnsFirstUsefulBeanValidationMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "displayName", "displayName is required"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        assertResponse(handler.malformedRequest(exception, () -> "actor"),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "displayName is required");
    }

    @Test
    void returnsFallbackForValidationWithoutUsefulMessage() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "displayName", " "));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                mock(MethodParameter.class), bindingResult);

        assertResponse(handler.malformedRequest(exception, null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request is invalid");
    }

    @Test
    void exposesExplicitUnknownFieldButHidesOtherJsonErrors() {
        HttpMessageNotReadableException unknownField = unreadable(
                new IllegalArgumentException("Unknown field: admin"));
        HttpMessageNotReadableException malformed = unreadable(new IllegalArgumentException("secret parser detail"));

        assertResponse(handler.malformedRequest(unknownField, null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Unknown field: admin");
        assertResponse(handler.malformedRequest(malformed, null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Malformed JSON request");
    }

    private static HttpMessageNotReadableException unreadable(Throwable cause) {
        return new HttpMessageNotReadableException("unreadable", cause, mock(HttpInputMessage.class));
    }

    private static void assertResponse(
            org.springframework.http.ResponseEntity<PATErrorResponse> response,
            HttpStatus status,
            String code,
            String message) {
        assertEquals(status, response.getStatusCode());
        assertEquals(new PATErrorResponse(code, message, NOW), response.getBody());
    }
}
