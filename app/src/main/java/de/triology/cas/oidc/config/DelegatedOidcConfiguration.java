package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.delegation.CesCustomDelegatedAuthenticationClientLogoutAction;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
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

@Configuration("DelegatedOidcConfiguration")
@ComponentScan("de.triology.cas.oidc")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfigureAfter(OidcConfiguration.class)
public class DelegatedOidcConfiguration {
    protected static final Logger LOG = LoggerFactory.getLogger(DelegatedOidcConfiguration.class);

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${cas.authn.pac4j.oidc[0].generic.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Bean
    @RefreshScope
    public Action delegatedAuthenticationClientLogoutAction(
            ObjectProvider<Clients> builtClients,
            ObjectProvider<SessionStore<JEEContext>> delegatedClientDistributedSessionStore) {
        String redirectURI = redirectUri.isEmpty() ? casServerPrefix + "/logout" : redirectUri;
        return new CesCustomDelegatedAuthenticationClientLogoutAction(builtClients.getObject(),
                delegatedClientDistributedSessionStore.getObject(), redirectURI);
    }
}
