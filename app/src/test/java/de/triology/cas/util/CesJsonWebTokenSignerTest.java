package de.triology.cas.util;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CesJsonWebTokenSigner}.
 */
class CesJsonWebTokenSignerTest {

    private RsaJsonWebKey jsonWebKey;

    @BeforeEach
    void setUp() throws Exception {
        jsonWebKey = RsaJwkGenerator.generateJwk(2048);
        jsonWebKey.setKeyId("test-key-id");
    }

    @Test
    void sign_DoesNotEmbedJwkHeader() {
        var header = headerOf(sign());

        assertFalse(header.contains("\"jwk\""),
                "signed token must not carry an embedded jwk header, got: " + header);
    }

    @Test
    void sign_KeepsKidHeaderSoTokenStaysVerifiableViaJwksUri() {
        var header = headerOf(sign());

        assertTrue(header.contains("\"kid\":\"test-key-id\""),
                "signed token must carry the kid header, got: " + header);
    }

    @Test
    void sign_SetsTypeAndCustomHeaders() {
        var header = headerOf(sign());

        assertTrue(header.contains("\"typ\":\"JWT\""), header);
        assertTrue(header.contains("\"client_id\":\"bluespice\""), header);
    }

    @Test
    void sign_ProducesSignatureVerifiableWithThePublicKey() throws Exception {
        var token = sign();

        var jws = new JsonWebSignature();
        jws.setCompactSerialization(token);
        jws.setKey(jsonWebKey.getPublicKey());

        assertTrue(jws.verifySignature(), "signature must verify against the public key");
        assertEquals("admin", JwtClaims.parse(jws.getPayload()).getSubject());
    }

    private String sign() {
        var claims = new JwtClaims();
        claims.setSubject("admin");
        claims.setIssuer("https://cas.example.com/cas/oidc");

        return CesJsonWebTokenSigner.builder()
                .key(jsonWebKey.getPrivateKey())
                .keyId(jsonWebKey.getKeyId())
                .algorithm(AlgorithmIdentifiers.RSA_USING_SHA256)
                .allowedAlgorithms(Set.of(AlgorithmIdentifiers.RSA_USING_SHA256))
                .mediaType("JWT")
                .headers(Map.of("client_id", "bluespice"))
                .build()
                .sign(claims);
    }

    private static String headerOf(final String token) {
        var encodedHeader = token.split("\\.")[0];
        return new String(Base64.getUrlDecoder().decode(encodedHeader), StandardCharsets.UTF_8);
    }
}
