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
 */
public record CreatePATResponse(
        UUID id,
        String userId,
        String displayName,
        String token,
        Instant createdAt,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Instant expiresAt,
        String scope) {

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
