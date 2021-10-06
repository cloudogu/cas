package de.triology.cas.oidc.services;

import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;

/**
 * This factory is responsible to create and to configure new OIDC services.
 */
public class CesOIDCServiceFactory extends CesOAuthServiceFactory {

    @Override
    protected OAuthRegisteredService createEmptyService() {
        return new OidcRegisteredService();
    }
}
