package de.triology.cas.pat.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Public, non-sensitive metadata of a personal access token.
 *
 * @param id unique token record identifier
 * @param userId owner of the token
 * @param displayName human-readable token name
 * @param createdAt creation timestamp in UTC
 * @param expiresAt expiration timestamp in UTC, or {@code null} for a non-expiring token
 * @param scope opaque permission scope supplied by User Management
 * @param description optional description text
 * @param owner optional LDAP user ID
 * @param expiryNotification whether expiration notifications are enabled (default true)
 */
public record PATMetadata(
        UUID id,
        String userId,
        String displayName,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Instant expiresAt,
        String scope,
        String description,
        String owner,
        boolean expiryNotification) {

    /**
     * Creates a PAT value with defaults for the optional additional metadata.
     */
    public PATMetadata(UUID id, String userId, String displayName,
                       Instant createdAt, Instant expiresAt, String scope) {
        this(id, userId, displayName, createdAt, expiresAt, scope, "", "", true);
    }
}
