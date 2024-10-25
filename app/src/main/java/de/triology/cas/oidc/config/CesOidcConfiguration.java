package de.triology.cas.oidc.config;

import de.triology.cas.ldap.UserManager;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.delegation.CesCustomDelegatedAuthenticationClientLogoutAction;
import de.triology.cas.oidc.beans.delegation.CesDelegatedClientUserProfileProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.config.OidcConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.session.SessionStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.webflow.execution.Action;

@Configuration("CesOidcConfiguration")
@AutoConfigureAfter(OidcConfiguration.class)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesOidcConfiguration {

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${cas.authn.pac4j.oidc[0].generic.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oidcCasClientRedirectActionBuilder() {
        LOGGER.debug("Create OIDC-OAuth client redirect action builder...");
        return new CesOidcClientRedirectActionBuilder();
    }

    @Bean
    @RefreshScope
    public Action delegatedAuthenticationClientLogoutAction(
            ObjectProvider<Clients> builtClients,
            ObjectProvider<SessionStore> delegatedClientDistributedSessionStore) {
        String redirectURI = redirectUri.isEmpty() ? casServerPrefix + "/logout" : redirectUri;
        return new CesCustomDelegatedAuthenticationClientLogoutAction(builtClients.getObject(),
                delegatedClientDistributedSessionStore.getObject(), redirectURI);
    }

    ConnectionFactory searchPooledLdapConnectionFactory(CasConfigurationProperties properties) {
        var ldapProperties = properties.getAuthn().getLdap().get(0);
        return LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);
    }

    @Bean
    @RefreshScope
    public DelegatedClientUserProfileProvisioner clientUserProfileProvisioner(final CasConfigurationProperties casProperties) {
        UserManager userResolver = new UserManager("ou=People,o=k3ces.local,dc=cloudogu,dc=com", searchPooledLdapConnectionFactory(casProperties));

        return new CesDelegatedClientUserProfileProvisioner(userResolver);
    }
}
