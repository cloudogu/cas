package de.triology.cas.pat.service;

/**
 * Wraps database failures that should be exposed as service-unavailable responses.
 */
public class PATStorageUnavailableException extends RuntimeException {
    /**
     * Creates a storage exception while retaining the internal database cause.
     * @param cause original persistence exception
     */
    public PATStorageUnavailableException(Throwable cause) {
        super("PAT storage is unavailable", cause);
    }
}
