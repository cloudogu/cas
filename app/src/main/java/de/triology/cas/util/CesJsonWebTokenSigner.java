package de.triology.cas.util;

import lombok.Builder;
import org.apereo.cas.util.crypto.IdentifiableKey;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import java.security.Key;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Signs JWT claims <em>without</em> embedding the public signing key as a {@code jwk} JOSE header.
 *
 * <p>This is a stripped-down copy of {@code org.apereo.cas.util.jwt.JsonWebTokenSigner} (CAS 8.0.1).
 * Since <a href="https://github.com/apereo/cas/commit/f9d143b25c22890700864860f34b453d3590aa1d">
 * apereo/cas@f9d143b</a> the upstream signer unconditionally adds the public half of the signing key
 * as a {@code jwk} header to every token it signs. Strict OIDC clients reject such tokens — the PHP
 * library {@code jumbojett/openid-connect-php} (>= 1.0.0) aborts with "Self signed JWK header is not
 * valid", which breaks the BlueSpice login against CAS 8.
 *
 * <p>The {@code kid} header is still set, so tokens remain verifiable against the keys published at
 * {@code jwks_uri}. Upstream offers no setting to turn the {@code jwk} header off; the pull request
 * proposing one (<a href="https://github.com/apereo/cas/pull/9525">apereo/cas#9525</a>) was closed
 * unreviewed by the stale bot. Drop this class once upstream provides such a setting.
 *
 * @see de.triology.cas.oidc.beans.CesOidcIdTokenSigningAndEncryptionService
 */
@Builder
public class CesJsonWebTokenSigner {

    @Builder.Default
    private final String keyId = UUID.randomUUID().toString();

    private final String algorithm;

    @Builder.Default
    private final String mediaType = "JWT";

    @Builder.Default
    private final Map<String, Object> headers = new LinkedHashMap<>();

    private final Key key;

    @Builder.Default
    private final Set<String> allowedAlgorithms = new LinkedHashSet<>();

    /**
     * Signs the given claims and returns the compact serialization of the resulting JWS.
     *
     * @param claims the claims to sign
     * @return the signed token
     */
    public String sign(final JwtClaims claims) {
        try {
            return sign(claims.toJson());
        } catch (JoseException e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }

    private String sign(final String payload) throws JoseException {
        var jws = new JsonWebSignature();
        jws.setPayload(payload);
        jws.setAlgorithmHeaderValue(algorithm);
        jws.setAlgorithmConstraints(algorithmConstraints());
        jws.setHeader("typ", mediaType);

        if (key instanceof IdentifiableKey identifiableKey) {
            jws.setKey(identifiableKey.getKey());
            jws.setKeyIdHeaderValue(identifiableKey.getId());
        } else {
            jws.setKey(key);
            if (keyId != null) {
                jws.setKeyIdHeaderValue(keyId);
            }
        }

        // Deliberately no jws.setJwkHeader(...) here — that is the whole point of this class.

        headers.forEach((header, value) -> jws.setHeader(header, value.toString()));
        return jws.getCompactSerialization();
    }

    private AlgorithmConstraints algorithmConstraints() {
        if (allowedAlgorithms.isEmpty() || allowedAlgorithms.contains("*")) {
            return AlgorithmConstraints.DISALLOW_NONE;
        }
        return new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT,
                allowedAlgorithms.toArray(new String[0]));
    }
}
