package de.triology.cas.pat.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated metadata used when creating a personal access token.
 *
 * @param displayName human-readable token name
 * @param expiresAt optional expiration timestamp
 * @param scope comma-separated list of paths, defaulting to {@code /*} when empty
 * @param description optional description text
 * @param owner optional LDAP user ID
 * @param expiryNotification whether expiration notifications are enabled (default true)
 */
public record CreatePATRequest(
        @NotBlank(message = "displayName is required")
        @Size(max = 255, message = "displayName must not exceed 255 characters")
        String displayName,
        Instant expiresAt,
        @Size(max = 1000, message = "scope must not exceed 1000 characters")
        String scope,
        String description,
        String owner,
        Boolean expiryNotification) {

    /**
     * Creates a PAT value with defaults for the optional additional metadata.
     */
    public CreatePATRequest(String displayName, Instant expiresAt, String scope) {
        this(displayName, expiresAt, scope, "", "", true);
    }

    /**
     * Rejects fields outside the create contract even when the global mapper accepts unknown properties.
     *
     * @param field unknown property name
     * @param value unknown property value
     */
    @JsonAnySetter
    public void rejectUnknownField(String field, Object value) {
        throw new IllegalArgumentException("Unknown field: " + field);
    }
}
