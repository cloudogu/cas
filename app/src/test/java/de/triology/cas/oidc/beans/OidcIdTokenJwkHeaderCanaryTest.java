package de.triology.cas.oidc.beans;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.discovery.OidcServerDiscoverySettings;
import org.apereo.cas.oidc.token.OidcIdTokenSigningAndEncryptionService;
import org.apereo.cas.services.OidcRegisteredService;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Watchdog on stock CAS, not our own code: CAS 8.0.2 stopped embedding the {@code jwk} JOSE
 * header that broke strict OIDC clients like BlueSpice, so this Dogu's custom signing override
 * (#373) was removed (#383). If this test ever fails, upstream is embedding it again and that
 * override needs to come back.
 */
class OidcIdTokenJwkHeaderCanaryTest {

    @Test
    void upstreamDoesNotEmbedJwkHeader() throws Exception {
        var jsonWebKey = RsaJwkGenerator.generateJwk(2048);
        jsonWebKey.setKeyId("test-key-id");

        var registeredService = new OidcRegisteredService();
        registeredService.setClientId("bluespice");
        registeredService.setServiceId("https://192.168.56.2/bluespice.*");
        registeredService.setIdTokenSigningAlg(AlgorithmIdentifiers.RSA_USING_SHA256);

        var discoverySettings = mock(OidcServerDiscoverySettings.class);
        when(discoverySettings.getIdTokenSigningAlgValuesSupported())
                .thenReturn(Set.of(AlgorithmIdentifiers.RSA_USING_SHA256));

        var claims = new JwtClaims();
        claims.setSubject("admin");
        claims.setIssuer("https://cas.example.com/cas/oidc");

        var upstream = new UpstreamSigningService(discoverySettings);
        var token = upstream.signTokenForTest(registeredService, claims, jsonWebKey);
        var encodedHeader = token.split("\\.")[0];
        var header = new String(Base64.getUrlDecoder().decode(encodedHeader), StandardCharsets.UTF_8);

        assertFalse(header.contains("\"jwk\""),
                "upstream CAS is expected to no longer embed a jwk header (fixed in CAS 8.0.2); "
                        + "if it does again, the removed jwk-suppression workaround is needed again");
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
