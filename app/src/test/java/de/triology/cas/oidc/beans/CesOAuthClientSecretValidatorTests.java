package de.triology.cas.oidc.beans;

import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesOAuthClientSecretValidator}.
 */
class CesOAuthClientSecretValidatorTests {

    private CesOAuthClientSecretValidator validator;

    @BeforeEach
    void setUp() {
        // CipherExecutor is not used in this logic but required for constructor
        @SuppressWarnings("unchecked")
        CipherExecutor<Serializable, String> cipherExecutor = mock(CipherExecutor.class);
        validator = new CesOAuthClientSecretValidator(cipherExecutor);
    }

    @Test
    void validate_ShouldReturnTrue_WhenSecretsMatch() {
        // given
        OAuthRegisteredService service = mock(OAuthRegisteredService.class);
        String rawSecret = "correctSecret";
        String hashedSecret = org.apache.commons.codec.digest.DigestUtils.sha256Hex(rawSecret);

        when(service.getClientSecret()).thenReturn(hashedSecret);

        // when
        boolean result = validator.validate(service, rawSecret);

        // then
        assertTrue(result, "Validator should return true when client secrets match");
    }

    @Test
    void validate_ShouldReturnFalse_WhenSecretsDoNotMatch() {
        // given
        OAuthRegisteredService service = mock(OAuthRegisteredService.class);
        String storedSecret = org.apache.commons.codec.digest.DigestUtils.sha256Hex("differentSecret");

        when(service.getClientSecret()).thenReturn(storedSecret);

        // when
        boolean result = validator.validate(service, "wrongSecret");

        // then
        assertFalse(result, "Validator should return false when client secrets do not match");
    }

    @Test
    void validate_ShouldReturnFalse_WhenStoredSecretIsBlank() {
        // given
        OAuthRegisteredService service = mock(OAuthRegisteredService.class);
        when(service.getClientSecret()).thenReturn(" "); // Blank string

        // when
        boolean result = validator.validate(service, "anySecret");

        // then
        assertFalse(result, "Validator should return false when stored client secret is blank");
    }

    @Test
    void isClientSecretExpired_ShouldAlwaysReturnFalse() {
        // given
        OAuthRegisteredService service = mock(OAuthRegisteredService.class);

        // when
        boolean expired = validator.isClientSecretExpired(service);

        // then
        assertFalse(expired, "Client secret should never expire according to this validator");
    }
}
