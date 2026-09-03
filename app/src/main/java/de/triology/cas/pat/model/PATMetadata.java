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
 */
public record PATMetadata(
        UUID id,
        String userId,
        String displayName,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Instant expiresAt,
        String scope) {
}
