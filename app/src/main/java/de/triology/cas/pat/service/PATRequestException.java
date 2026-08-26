package de.triology.cas.pat.service;

/**
 * Signals invalid client input detected by PAT domain validation.
 */
public class PATRequestException extends RuntimeException {
    /**
     * Creates an invalid-request exception.
     * @param message client-safe exception message
     */
    public PATRequestException(String message) {
        super(message);
    }
}
