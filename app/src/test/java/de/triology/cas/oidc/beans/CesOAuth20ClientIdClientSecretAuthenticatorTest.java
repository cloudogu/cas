package de.triology.cas.oidc.beans;

import de.triology.cas.oidc.services.CasOidcRegisteredService;
import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.CesServiceData;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.junit.Test;

import static de.triology.cas.oidc.services.CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID;
import static de.triology.cas.oidc.services.CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CesOAuth20ClientIdClientSecretAuthenticator}.
 */
public class CesOAuth20ClientIdClientSecretAuthenticatorTest {

    /**
     * Positive Test for {@link CesOAuth20ClientIdClientSecretAuthenticator#checkClientSecret(OAuthRegisteredService, String)} ()}.
     */
    @Test
    public void checkClientSecret_Test_CorrectClientSecret() throws Exception {
        //given
        String expectedClientSecret = "supersecret";
        String expectedClientSecretHashSHA256 = "f75778f7425be4db0369d09af37a6c2b9a83dea0e53e7bd57412e4b060e607f7";
        var factory = new CesOAuthServiceFactory<>(CasOidcRegisteredService::new);
        CesServiceData data = new CesServiceData("testService", factory);
        data.getAttributes().put(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, expectedClientSecretHashSHA256);
        data.getAttributes().put(ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "placeholderClientID");
        OAuthRegisteredService service = (OAuthRegisteredService) factory.createNewService(1, "test.de", null, data);

        //when
        boolean isCorrect = CesOAuth20ClientIdClientSecretAuthenticator.checkClientSecret(service, expectedClientSecret);

        //then
        assertTrue(isCorrect);
    }

    /**
     * Negative Test for {@link CesOAuth20ClientIdClientSecretAuthenticator#checkClientSecret(OAuthRegisteredService, String)} ()}.
     */
    @Test
    public void checkClientSecret_Test_IncorrectClientSecret() throws Exception {
        //given
        String expectedClientSecret = "supersecret";
        String expectedClientSecretHashSHA256 = "this is the wrong hash";
        var factory = new CesOAuthServiceFactory<>(CasOidcRegisteredService::new);
        CesServiceData data = new CesServiceData("testService", factory);
        data.getAttributes().put(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, expectedClientSecretHashSHA256);
        data.getAttributes().put(ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "placeholderClientID");
        OAuthRegisteredService service = (OAuthRegisteredService) factory.createNewService(1, "test.de", null, data);

        //when
        boolean isCorrect = CesOAuth20ClientIdClientSecretAuthenticator.checkClientSecret(service, expectedClientSecret);

        //then
        assertFalse(isCorrect);
    }

}
