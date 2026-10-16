package de.triology.cas.pat.service;

/**
 * Signals that a PAT does not exist for the requested owner and identifier.
 */
public class PATOperationException extends RuntimeException {
    /**
     * Creates a not-found exception with the standard PAT message.
     */
    public PATOperationException() {
        super("PAT Database operation failed");
    }
}
