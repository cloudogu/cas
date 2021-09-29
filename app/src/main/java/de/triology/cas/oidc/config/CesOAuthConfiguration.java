package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.CesOAuth20ClientIdClientSecretAuthenticator;
import de.triology.cas.oidc.beans.CesOAuthProfileRenderer;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.pac4j.core.credentials.UsernamePasswordCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration("CesOAuthConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.oidc")
public class CesOAuthConfiguration {

    @Bean
    @RefreshScope
    public OAuth20UserProfileViewRenderer oauthUserProfileViewRenderer() {
        return new CesOAuthProfileRenderer();
    }

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @RefreshScope
    public Authenticator<UsernamePasswordCredentials> oAuthClientAuthenticator(
            ServicesManager servicesManager,
            TicketRegistry ticketRegistry,
            ServiceFactory webApplicationServiceFactory,
            AuditableExecution registeredServiceAccessStrategyEnforcer,
            PrincipalResolver defaultPrincipalResolver,
            CipherExecutor<Serializable, java.lang.String> oauthRegisteredServiceCipherExecutor) {
        return new CesOAuth20ClientIdClientSecretAuthenticator(servicesManager,
                webApplicationServiceFactory,
                registeredServiceAccessStrategyEnforcer,
                oauthRegisteredServiceCipherExecutor,
                ticketRegistry,
                defaultPrincipalResolver);
    }

}

