package de.triology.cas.oidc.config;

import de.triology.cas.oidc.CustomDelegatedAuthenticationClientLogoutAction;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.support.RequiredProperty;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.webflow.execution.Action;

@Configuration("CesOidcConfiguration")
@ComponentScan("de.triology.cas.oidc")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CesOidcConfiguration {
    private final ObjectProvider<SessionStore> delegatedClientDistributedSessionStore;
    private final ObjectProvider<Clients> builtClients;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public CesOidcConfiguration(@Qualifier("builtClients") ObjectProvider<Clients> builtClients,
                                @Qualifier("delegatedClientDistributedSessionStore") ObjectProvider<SessionStore> delegatedClientDistributedSessionStore) {
        this.builtClients = builtClients;
        this.delegatedClientDistributedSessionStore = delegatedClientDistributedSessionStore;
    }

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${cas.authn.pac4j.oidc[0].generic.redirect-uri:#{\"\"}}")
    private String redirectUri;

    private String getRedirectUri(){
        return redirectUri.isEmpty() ? casServerPrefix + "/logout" : redirectUri;
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "delegatedAuthenticationClientLogoutAction")
    public Action delegatedAuthenticationClientLogoutAction() {
        return new CustomDelegatedAuthenticationClientLogoutAction(builtClients.getObject(),
                delegatedClientDistributedSessionStore.getObject(), getRedirectUri());
    }
}
