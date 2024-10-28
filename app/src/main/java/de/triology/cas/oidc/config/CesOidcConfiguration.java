package de.triology.cas.oidc.config;

import de.triology.cas.ldap.UserManager;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.delegation.AttributeMapping;
import de.triology.cas.oidc.beans.delegation.CesCustomDelegatedAuthenticationClientLogoutAction;
import de.triology.cas.oidc.beans.delegation.CesDelegatedAuthenticationPreProcessor;
import de.triology.cas.oidc.beans.delegation.CesDelegatedClientUserProfileProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.DelegatedAuthenticationPreProcessor;
import org.apereo.cas.config.OidcConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.PooledConnectionFactory;
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

import java.util.List;

@Configuration("CesOidcConfiguration")
@AutoConfigureAfter(OidcConfiguration.class)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesOidcConfiguration {

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${ces.delegation.oidc.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Value("${ces.delegation.oidc.attributeMapping:#{\"\"}}")
    private String attributesMappingsString;

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

    @Bean
    @RefreshScope
    public DelegatedClientUserProfileProvisioner clientUserProfileProvisioner(final CasConfigurationProperties casProperties) {
        UserManager userManager = getUserManager(casProperties);

        return new CesDelegatedClientUserProfileProvisioner(userManager);
    }

    @Bean
    @RefreshScope
    public DelegatedAuthenticationPreProcessor delegatedAuthenticationPreProcessor(final CasConfigurationProperties casProperties) {
        UserManager userManager = getUserManager(casProperties);
        List<AttributeMapping> attributeMappings = AttributeMapping.fromPropertyString(attributesMappingsString);

        return new CesDelegatedAuthenticationPreProcessor(attributeMappings, userManager);
    }

    private static UserManager getUserManager(CasConfigurationProperties casProperties) {
        LdapAuthenticationProperties ldapProperties = casProperties.getAuthn().getLdap().getFirst();
        PooledConnectionFactory connectionFactory = LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);

        return new UserManager(ldapProperties.getBaseDn(), connectionFactory);
    }
}
