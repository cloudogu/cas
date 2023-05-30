package de.triology.cas.oidc.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.validator.OAuth20ClientSecretValidator;
import org.apereo.cas.util.crypto.CipherExecutor;

import java.io.Serializable;

/**
 * This is {@link CesOAuthClientSecretValidator}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@Slf4j
@RequiredArgsConstructor
@Getter
public class CesOAuthClientSecretValidator implements OAuth20ClientSecretValidator {
    private final CipherExecutor<Serializable, String> cipherExecutor;

    @Override
    public boolean validate(final OAuthRegisteredService registeredService, final String clientSecret) {
        LOGGER.debug("Found: [{}] in secret check", registeredService);
        var storedClientSecretHash = registeredService.getClientSecret();
        if (StringUtils.isBlank(storedClientSecretHash)) {
            LOGGER.debug("The client secret is not defined for the registered service [{}]", registeredService.getName());
            return false;
        }

        String clientSecretHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(clientSecret);
        LOGGER.debug("Check Secrets:\n\nInput-Secret: {}\nInput-Secret-Hash: {}\nService-Hash: {}", clientSecret, clientSecretHash, registeredService.getClientSecret());

        if (!StringUtils.equals(storedClientSecretHash, clientSecretHash)) {
            LOGGER.error("Wrong client secret for service: [{}]", registeredService.getServiceId());
            return false;
        }
        return true;
    }

    @Override
    public boolean isClientSecretExpired(final OAuthRegisteredService registeredService) {
        return false;
    }
}