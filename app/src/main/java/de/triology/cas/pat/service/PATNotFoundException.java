package de.triology.cas.pat.service;

/**
 * Signals that a PAT does not exist for the requested owner and identifier.
 */
public class PATNotFoundException extends RuntimeException {
    /**
     * Creates a not-found exception with the standard PAT message.
     */
    public PATNotFoundException() {
        super("PAT not found");
    }
}
