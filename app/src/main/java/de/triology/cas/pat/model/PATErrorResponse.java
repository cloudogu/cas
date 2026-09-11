package de.triology.cas.pat.model;

import java.time.Instant;

/**
 * Stable JSON error representation returned by the PAT API.
 *
 * @param code machine-readable error code
 * @param message safe, human-readable error description
 * @param timestamp time at which the error response was created
 */
public record PATErrorResponse(String code, String message, Instant timestamp) {
    /**
     * Creates an error response with a handler-supplied timestamp.
     *
     * @param code machine-readable error code
     * @param message safe error description
     * @param timestamp response creation time
     * @return timestamped error response
     */
    public static PATErrorResponse of(String code, String message, Instant timestamp) {
        return new PATErrorResponse(code, message, timestamp);
    }
}
