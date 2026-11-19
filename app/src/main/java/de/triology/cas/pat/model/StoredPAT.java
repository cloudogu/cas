package de.triology.cas.pat.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Complete persistence representation of a PAT with an irreversible token fingerprint.
 *
 * @param id unique token record identifier
 * @param userId owner of the token
 * @param displayName human-readable token name
 * @param tokenFingerprint SHA-256 fingerprint reserved for future validation
 * @param createdAt creation timestamp in UTC
 * @param expiresAt expiration timestamp, or {@code null}
 * @param scope opaque permission scope
 * @param description optional description text
 * @param owner optional LDAP user ID
 * @param expiryNotification whether expiration notifications are enabled (default true)
 */
public record StoredPAT(
        UUID id,
        String userId,
        String displayName,
        PATFingerprint tokenFingerprint,
        Instant createdAt,
        Instant expiresAt,
        String scope,
        String description,
        String owner,
        boolean expiryNotification) {

    /**
     * Creates a PAT value with defaults for the optional additional metadata.
     */
    public StoredPAT(UUID id, String userId, String displayName, PATFingerprint tokenFingerprint,
                     Instant createdAt, Instant expiresAt, String scope) {
        this(id, userId, displayName, tokenFingerprint, createdAt, expiresAt, scope, "", "", true);
    }

    /**
     * Returns a log-safe representation that masks the token fingerprint.
     *
     * @return stored token description without secret material
     */
    @Override
    public String toString() {
        return "StoredPAT[id=" + id + ", userId=" + userId + ", displayName=" + displayName
                + ", tokenFingerprint=******, createdAt=" + createdAt + ", expiresAt=" + expiresAt
                + ", scope=" + scope + "]";
    }
}
