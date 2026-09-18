package de.triology.cas.oidc.beans;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.triology.cas.util.CesJsonWebTokenSigner;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.discovery.OidcServerDiscoverySettings;
import org.apereo.cas.oidc.issuer.OidcIssuerService;
import org.apereo.cas.oidc.jwks.OidcJsonWebKeyCacheKey;
import org.apereo.cas.oidc.token.OidcIdTokenSigningAndEncryptionService;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwt.JwtClaims;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Signs id tokens without the {@code jwk} JOSE header that CAS 8 adds by default.
 *
 * <p>Everything but {@link #signToken} is inherited from {@link OidcIdTokenSigningAndEncryptionService};
 * only the signing step swaps the upstream signer for {@link CesJsonWebTokenSigner}, which leaves the
 * {@code jwk} header out.
 */
@Slf4j
public class CesOidcIdTokenSigningAndEncryptionService extends OidcIdTokenSigningAndEncryptionService {

    public CesOidcIdTokenSigningAndEncryptionService(
            final LoadingCache<OidcJsonWebKeyCacheKey, JsonWebKeySet> defaultJsonWebKeystoreCache,
            final LoadingCache<OidcJsonWebKeyCacheKey, Optional<JsonWebKeySet>> serviceJsonWebKeystoreCache,
            final OidcIssuerService issuerService,
            final OidcServerDiscoverySettings discoverySettings,
            final CasConfigurationProperties casProperties) {
        super(defaultJsonWebKeystoreCache, serviceJsonWebKeystoreCache, issuerService, discoverySettings, casProperties);
    }

    @Override
    protected String signToken(final OAuthRegisteredService registeredService,
                               final JwtClaims claims,
                               final PublicJsonWebKey jsonWebKey) {
        LOGGER.debug("Signing id token for service [{}] without a jwk header", registeredService.getServiceId());
        return CesJsonWebTokenSigner.builder()
                .key(Optional.ofNullable(jsonWebKey)
                        .map(PublicJsonWebKey::getPrivateKey)
                        .orElse(null))
                .keyId(Optional.ofNullable(jsonWebKey)
                        .map(PublicJsonWebKey::getKeyId)
                        .orElseGet(() -> UUID.randomUUID().toString()))
                .algorithm(getJsonWebKeySigningAlgorithm(registeredService, jsonWebKey))
                .allowedAlgorithms(new LinkedHashSet<>(getAllowedSigningAlgorithms(registeredService)))
                .mediaType(getSigningMediaType())
                .headers(Map.of(OAuth20Constants.CLIENT_ID, registeredService.getClientId()))
                .build()
                .sign(claims);
    }
}
