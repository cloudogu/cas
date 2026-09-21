package de.triology.cas.oidc.beans;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.discovery.OidcServerDiscoverySettings;
import org.apereo.cas.oidc.token.OidcIdTokenSigningAndEncryptionService;
import org.apereo.cas.services.OidcRegisteredService;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CesOidcIdTokenSigningAndEncryptionService}.
 */
class CesOidcIdTokenSigningAndEncryptionServiceTest {

    private RsaJsonWebKey jsonWebKey;
    private OidcRegisteredService registeredService;
    private OidcServerDiscoverySettings discoverySettings;

    @BeforeEach
    void setUp() throws Exception {
        jsonWebKey = RsaJwkGenerator.generateJwk(2048);
        jsonWebKey.setKeyId("test-key-id");

        registeredService = new OidcRegisteredService();
        registeredService.setClientId("bluespice");
        registeredService.setServiceId("https://192.168.56.2/bluespice.*");
        registeredService.setIdTokenSigningAlg(AlgorithmIdentifiers.RSA_USING_SHA256);

        discoverySettings = mock(OidcServerDiscoverySettings.class);
        when(discoverySettings.getIdTokenSigningAlgValuesSupported())
                .thenReturn(Set.of(AlgorithmIdentifiers.RSA_USING_SHA256));
    }

    @Test
    void signToken_OmitsJwkHeader() {
        var service = new CesOidcIdTokenSigningAndEncryptionService(null, null, null,
                discoverySettings, new CasConfigurationProperties());

        var header = headerOf(service.signToken(registeredService, claims(), jsonWebKey));

        assertFalse(header.contains("\"jwk\""),
                "id token must not carry an embedded jwk header, got: " + header);
        assertTrue(header.contains("\"kid\":\"test-key-id\""),
                "id token must keep the kid header, got: " + header);
    }

    @Test
    void signToken_KeyWithoutKeyId_FallsBackToARandomKid() {
        jsonWebKey.setKeyId(null);
        var service = new CesOidcIdTokenSigningAndEncryptionService(null, null, null,
                discoverySettings, new CasConfigurationProperties());

        var header = headerOf(service.signToken(registeredService, claims(), jsonWebKey));

        assertTrue(header.contains("\"kid\""), "a random kid must be generated, got: " + header);
        assertFalse(header.contains("\"jwk\""), header);
    }

    /**
     * Documents the upstream behaviour this class works around. If this test ever fails, CAS has
     * stopped embedding the jwk header on its own — at which point
     * {@link CesOidcIdTokenSigningAndEncryptionService} and {@code CesJsonWebTokenSigner} can go.
     */
    @Test
    void upstreamStillEmbedsJwkHeader() {
        var upstream = new UpstreamSigningService(discoverySettings);

        var header = headerOf(upstream.signTokenForTest(registeredService, claims(), jsonWebKey));

        assertTrue(header.contains("\"jwk\""),
                "upstream CAS is expected to embed a jwk header; if it no longer does, drop the workaround");
    }

    private static JwtClaims claims() {
        var claims = new JwtClaims();
        claims.setSubject("admin");
        claims.setIssuer("https://cas.example.com/cas/oidc");
        return claims;
    }

    private static String headerOf(final String token) {
        var encodedHeader = token.split("\\.")[0];
        return new String(Base64.getUrlDecoder().decode(encodedHeader), StandardCharsets.UTF_8);
    }

    /**
     * Exposes the protected {@code signToken} of the stock CAS implementation, which is otherwise
     * inaccessible from this package.
     */
    private static final class UpstreamSigningService extends OidcIdTokenSigningAndEncryptionService {
        private UpstreamSigningService(final OidcServerDiscoverySettings discoverySettings) {
            super(null, null, null, discoverySettings, new CasConfigurationProperties());
        }

        private String signTokenForTest(final OidcRegisteredService service,
                                        final JwtClaims claims,
                                        final RsaJsonWebKey jsonWebKey) {
            return signToken(service, claims, jsonWebKey);
        }
    }
}
