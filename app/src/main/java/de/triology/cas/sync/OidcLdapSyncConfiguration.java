package de.triology.cas.sync;

import de.triology.cas.ldap.LdapConfiguration;
import de.triology.cas.ldap.LdapOperationFactory;
import de.triology.cas.ldap.UserManager;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/** Configures the SCIM client and LDAP synchronization beans. */
@AutoConfiguration(after = LdapConfiguration.class)
@EnableScheduling
@EnableConfigurationProperties(OidcLdapSyncProperties.class)
public class OidcLdapSyncConfiguration {
    /** Creates the HTTP client used to retrieve SCIM group members. */
    @Bean
    @ConditionalOnProperty(prefix = "oidc.ldap-sync", name = "enabled", havingValue = "true")
    ScimClient scimClient(RestClient.Builder restClientBuilder, OidcLdapSyncProperties properties) {
        return new ScimClient(restClientBuilder, properties.getScim(),
                properties.getMapping().getScimIdentityAttribute());
    }

    /** Creates the LDAP user manager used by the synchronizer. */
    @Bean
    @ConditionalOnProperty(prefix = "oidc.ldap-sync", name = "enabled", havingValue = "true")
    UserManager oidcLdapUserManager(CasConfigurationProperties casProperties) {
        LdapAuthenticationProperties ldapProperties = casProperties.getAuthn().getLdap().getFirst();
        ConnectionFactory connectionFactory = LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);
        return new UserManager(ldapProperties.getBaseDn(), new LdapOperationFactory(connectionFactory));
    }

    /** Creates the scheduled synchronization worker when OIDC/LDAP synchronization is enabled. */
    @Bean
    @ConditionalOnProperty(prefix = "oidc.ldap-sync", name = "enabled", havingValue = "true")
    ScheduledOidcLdapSynchronizer scheduledOidcLdapSynchronizer(ScimClient scimClient, UserManager userManager,
                                                                 OidcLdapSyncProperties properties) {
        return new ScheduledOidcLdapSynchronizer(scimClient, userManager, properties);
    }

}
