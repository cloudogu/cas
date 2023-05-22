package de.triology.cas.oidc.config;

import de.triology.cas.ldap.resolvers.AllUserResolver;
import de.triology.cas.ldap.resolvers.UserResolver;
import de.triology.cas.oidc.beans.CESDelegatedClientAuthenticationHandler;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.delegation.CesCustomDelegatedAuthenticationClientLogoutAction;
import lombok.val;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.session.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class CesOidcConfiguration {
    protected static final Logger LOG = LoggerFactory.getLogger(CesOidcConfiguration.class);

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${cas.authn.pac4j.oidc[0].generic.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oidcCasClientRedirectActionBuilder() {
        LOG.debug("Create OIDC-OAuth client redirect action builder...");
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

    @RefreshScope()
    @Bean
    public AuthenticationHandler clientAuthenticationHandler(
            final CasConfigurationProperties casProperties,
            @Qualifier("clientPrincipalFactory") final PrincipalFactory clientPrincipalFactory,
            @Qualifier("builtClients") final Clients builtClients,
            @Qualifier(DelegatedClientUserProfileProvisioner.BEAN_NAME) final DelegatedClientUserProfileProvisioner clientUserProfileProvisioner,
            @Qualifier("delegatedClientDistributedSessionStore") final SessionStore delegatedClientDistributedSessionStore,
            @Qualifier(ServicesManager.BEAN_NAME) final ServicesManager servicesManager) {
        val allUserResolver = new UserResolver("ou=People,o=ces.local,dc=cloudogu,dc=com", searchPooledLdapConnectionFactory(casProperties));
        val pac4j = casProperties.getAuthn().getPac4j().getCore();
        val h = new CESDelegatedClientAuthenticationHandler(
                pac4j.getName(),
                pac4j.getOrder(),
                servicesManager,
                clientPrincipalFactory,
                builtClients,
                clientUserProfileProvisioner,
                delegatedClientDistributedSessionStore,
                allUserResolver
        );
        h.setTypedIdUsed(pac4j.isTypedIdUsed());
        h.setPrincipalAttributeId(pac4j.getPrincipalAttributeId());
        return h;
    }
}
