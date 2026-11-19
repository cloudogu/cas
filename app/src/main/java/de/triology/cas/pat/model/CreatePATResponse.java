package de.triology.cas.pat.model;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response returned exactly once after a PAT has been generated.
 * The {@code token} component contains the only API-visible cleartext representation.
 *
 * @param id unique token record identifier
 * @param userId owner of the token
 * @param displayName human-readable token name
 * @param token newly generated cleartext token
 * @param createdAt creation timestamp in UTC
 * @param expiresAt expiration timestamp, or {@code null}
 * @param scope opaque permission scope
 * @param description optional description text
 * @param owner optional LDAP user ID
 * @param expiryNotification whether expiration notifications are enabled (default true)
 */
public record CreatePATResponse(
        UUID id,
        String userId,
        String displayName,
        String token,
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
    public CreatePATResponse(UUID id, String userId, String displayName, String token,
                             Instant createdAt, Instant expiresAt, String scope) {
        this(id, userId, displayName, token, createdAt, expiresAt, scope, "", "", true);
    }

    /**
     * Returns a log-safe representation that masks the cleartext token.
     *
     * @return response description without secret material
     */
    @Override
    public String toString() {
        return "CreatePATResponse[id=" + id + ", userId=" + userId + ", displayName=" + displayName
                + ", token=******, createdAt=" + createdAt + ", expiresAt=" + expiresAt + ", scope=" + scope + "]";
    }
}
