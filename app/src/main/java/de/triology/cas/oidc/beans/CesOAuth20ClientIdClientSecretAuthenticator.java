package de.triology.cas.oidc.beans;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.authenticator.OAuth20ClientIdClientSecretAuthenticator;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.support.oauth.validator.DefaultOAuth20ClientSecretValidator;
import org.apereo.cas.support.oauth.validator.OAuth20ClientSecretValidator;
import org.apereo.cas.support.oauth.web.DefaultOAuth20RequestParameterResolver;
import org.apereo.cas.support.oauth.web.OAuth20RequestParameterResolver;
import org.apereo.cas.support.oauth.web.response.accesstoken.response.OAuth20JwtAccessTokenCipherExecutor;
import org.apereo.cas.ticket.accesstoken.OAuth20JwtBuilder;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.cipher.DefaultTicketCipherExecutor;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.exception.CredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

/**
 * We override this Beans as we need to hash the client secret. The original Bean has not allowed this behaviour.
 */
public class CesOAuth20ClientIdClientSecretAuthenticator extends OAuth20ClientIdClientSecretAuthenticator {

    public CesOAuth20ClientIdClientSecretAuthenticator(ServicesManager servicesManager, ServiceFactory<WebApplicationService> webApplicationServiceServiceFactory, AuditableExecution registeredServiceAccessStrategyEnforcer, TicketRegistry ticketRegistry, PrincipalResolver principalResolver, OAuth20RequestParameterResolver requestParameterResolver, OAuth20ClientSecretValidator clientSecretValidator) {
        super(servicesManager, webApplicationServiceServiceFactory, registeredServiceAccessStrategyEnforcer, ticketRegistry, principalResolver, requestParameterResolver, clientSecretValidator);
    }

    protected static final Logger LOG = LoggerFactory.getLogger(CesOAuth20ClientIdClientSecretAuthenticator.class);

    /**
     * Validate credentials.
     *
     * @param credentials       the credentials
     * @param registeredService the registered service
     * @param context           the context
     */
    @Override
    protected void validateCredentials(final UsernamePasswordCredentials credentials,
                                       final OAuthRegisteredService registeredService,
                                       final WebContext context,
                                       final SessionStore sessionStore) {
        if (!checkClientSecret(registeredService, credentials.getPassword())) {
            throw new CredentialsException("Client Credentials provided is not valid for registered service: " + registeredService.getName());
        }
    }

    /**
     * Check the client secret by hashing the cleartext secret and comparing it to the hashed secret known to CAS.
     *
     * @param registeredService the registered service
     * @param clientSecret      the client secret
     * @return whether the secret is valid
     */
    public static boolean checkClientSecret(final OAuthRegisteredService registeredService, final String clientSecret) {
        LOG.debug("Found: [{}] in secret check", registeredService);
        var storedClientSecretHash = registeredService.getClientSecret();
        if (StringUtils.isBlank(storedClientSecretHash)) {
            LOG.debug("The client secret is not defined for the registered service [{}]", registeredService.getName());
            return false;
        }

        String clientSecretHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(clientSecret);
        LOG.debug("Check Secrets:\n\nInput-Secret: {}\nInput-Secret-Hash: {}\nService-Hash: {}", clientSecret, clientSecretHash, registeredService.getClientSecret());

        if (!StringUtils.equals(storedClientSecretHash, clientSecretHash)) {
            LOG.error("Wrong client secret for service: [{}]", registeredService.getServiceId());
            return false;
        }
        return true;
    }
}
