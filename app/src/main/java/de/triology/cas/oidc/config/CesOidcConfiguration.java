package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.CesCustomDelegatedAuthenticationClientLogoutAction;
import de.triology.cas.oidc.beans.CesOAuth20ClientIdClientSecretAuthenticator;
import de.triology.cas.oidc.beans.CesOAuthProfileRenderer;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.webflow.execution.Action;

import java.io.Serializable;

@Configuration("CesOidcConfiguration")
@ComponentScan("de.triology.cas.oidc")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfigureAfter(OidcConfiguration.class)
public class CesOidcConfiguration {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CesOidcConfiguration.class);

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${cas.authn.pac4j.oidc[0].generic.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Bean
    @RefreshScope
    public OAuth20UserProfileViewRenderer oauthUserProfileViewRenderer() {
        return new CesOAuthProfileRenderer();
    }

    @Bean
    @RefreshScope
    public Authenticator<UsernamePasswordCredentials> oAuthClientAuthenticator(
            ObjectProvider<ServicesManager> servicesManager,
            ObjectProvider<TicketRegistry> ticketRegistry,
            ObjectProvider<ServiceFactory> webApplicationServiceFactory,
            ObjectProvider<AuditableExecution> registeredServiceAccessStrategyEnforcer,
            ObjectProvider<PrincipalResolver> defaultPrincipalResolver,
            ObjectProvider<CipherExecutor<Serializable, String>> oauthRegisteredServiceCipherExecutor) {
        return new CesOAuth20ClientIdClientSecretAuthenticator(servicesManager.getObject(),
                webApplicationServiceFactory.getObject(),
                registeredServiceAccessStrategyEnforcer.getObject(),
                oauthRegisteredServiceCipherExecutor.getObject(),
                ticketRegistry.getObject(),
                defaultPrincipalResolver.getObject());
    }

    @Bean
    @RefreshScope
    public Action delegatedAuthenticationClientLogoutAction(
            ObjectProvider<Clients> builtClients,
            ObjectProvider<SessionStore<JEEContext>> delegatedClientDistributedSessionStore) {
        String redirectURI = redirectUri.isEmpty() ? casServerPrefix + "/logout" : redirectUri;
        return new CesCustomDelegatedAuthenticationClientLogoutAction(builtClients.getObject(),
                delegatedClientDistributedSessionStore.getObject(), redirectURI);
    }

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oauthCasClientRedirectActionBuilder() {
        LOGGER.debug("Create CES-OAuth client redirect action builder...");
        return new CesOidcClientRedirectActionBuilder();
    }

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oidcCasClientRedirectActionBuilder() {
        LOGGER.debug("Create OIDC-OAuth client redirect action builder...");
        return new CesOidcClientRedirectActionBuilder();
    }
}
