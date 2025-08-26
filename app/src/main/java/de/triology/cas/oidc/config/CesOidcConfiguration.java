package de.triology.cas.oidc.config;

import de.triology.cas.authentication.LegacyDefaultAuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import de.triology.cas.ldap.LdapOperationFactory;
import de.triology.cas.ldap.UserManager;
import de.triology.cas.oidc.beans.CesOAuthProfileRenderer;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.delegation.AttributeMapping;
import de.triology.cas.oidc.beans.delegation.CesCustomDelegatedAuthenticationClientLogoutAction;
import de.triology.cas.oidc.beans.delegation.CesDelegatedAuthenticationPreProcessor;
import de.triology.cas.oidc.beans.delegation.CesDelegatedClientUserProfileProvisioner;
import de.triology.cas.oidc.beans.delegation.CesDelegatedOidcClientsProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.authentication.principal.DelegatedAuthenticationPreProcessor;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.PooledConnectionFactory;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.webflow.execution.Action;
import org.apereo.cas.config.CasOidcAutoConfiguration;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;

import org.apereo.cas.pac4j.client.DelegatedIdentityProviderFactory;
import org.apereo.cas.pac4j.client.DelegatedIdentityProviders;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Configuration("CesOidcConfiguration")
@AutoConfigureAfter(CasOidcAutoConfiguration.class)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesOidcConfiguration {

    @Value("${cas.server.prefix:#{\"\"}}")
    private String casServerPrefix;

    @Value("${ces.delegation.oidc.redirect-uri:#{\"\"}}")
    private String redirectUri;

    @Value("${ces.delegation.oidc.attributeMapping:#{\"\"}}")
    private String attributesMappingsString;

    @Value("${ces.delegation.oidc.allowedGroups:#{\"\"}}")
    private String allowedGroupsConfigString;

    @Value("${ces.delegation.oidc.initialAdminUsernames:#{\"\"}}")
    private String initialAadminUsernamesConfigString;

    @Value("${ces.delegation.oidc.adminGroups:#{\"\"}}")
    private String adminGroupsConfigString;
    

    /**
     * Binds all OIDC clients from cas.properties into a Java object.
     *
     * This bean loads the list of OIDC client configurations from properties starting with:
     *   ces.delegation.oidc.clients[...]
     * It is a wrapper around a list, because Spring Boot needs a concrete type to bind into.
     *
     * As of CAS 7.1, delegated clients are expected to be provided explicitly and
     * are no longer built automatically from properties (`cas.authn.pac4j.*`).
     * 
     * → Without your own manual bean, the list of available OIDC clients remains empty!
     * @return CesDelegatedOidcClientsProperties containing the list of all defined clients
     */
    @Bean
    @RefreshScope
    @ConfigurationProperties(prefix = "ces.delegation.oidc")
    public CesDelegatedOidcClientsProperties cesDelegatedOidcClientsProperties() {
        return new CesDelegatedOidcClientsProperties();
    }

    @Bean
    @RefreshScope
    public OAuth20UserProfileViewRenderer oauthUserProfileViewRenderer() {
        return new CesOAuthProfileRenderer();
    }
    
    @Bean
    @Primary
    @RefreshScope
    public AuthenticationEventExecutionPlan authenticationEventExecutionPlan(
        @Qualifier("defaultAuthenticationHandlerResolver")
        AuthenticationHandlerResolver authenticationHandlerResolver, 
        TenantExtractor tenantExtractor) {
        return new LegacyDefaultAuthenticationEventExecutionPlan(authenticationHandlerResolver, tenantExtractor);
    }


    /**
     * Creates OIDC client objects dynamically from the loaded properties.
     *
     * This bean is responsible for taking the list from cesDelegatedOidcClientsProperties and building actual pac4j OidcClient instances.
     * These clients will later be available to CAS for login and authentication.
     * Background CAS 7.1+:
     * As of this version, CAS expects an explicit `DelegatedIdentityProviderFactory`,
     * which provides delegated clients.
     * 
     * Previously (CAS 6.x / 7.0) clients were automatically loaded from properties,
     * now a factory bean must be **manually provided**.
     * It implements DelegatedIdentityProviderFactory, which CAS expects for dynamic delegated client support.
     */
    @Bean
    @Primary
    @RefreshScope
    public DelegatedIdentityProviderFactory customDelegatedClientFactory(
        CasConfigurationProperties casProperties,
        Cache<String, Collection<BaseClient>> pac4jDelegatedClientFactoryCache,
        ConfigurableApplicationContext applicationContext,
        CesDelegatedOidcClientsProperties cesDelegatedOidcClientsProperties
    ) {
        return new DelegatedIdentityProviderFactory() {
            @Override
            public Collection<BaseClient> build() {
                LOGGER.debug("Creating delegated clients from ces.delegation.oidc.clients...");
        
                List<BaseClient> clients = new ArrayList<>();
        
                for (var clientProps : cesDelegatedOidcClientsProperties.getClients()) {
                    if (StringUtils.isBlank(clientProps.getDiscoveryUri()) ||
                        StringUtils.isBlank(clientProps.getClientId()) ||
                        StringUtils.isBlank(clientProps.getClientSecret())) {
                        LOGGER.error("Invalid configuration for OIDC client; skipping {}", clientProps.getClientName());
                        continue;
                    }
        
                    var config = new OidcConfiguration();
                    config.setDiscoveryURI(clientProps.getDiscoveryUri());
                    config.setClientId(clientProps.getClientId());
                    config.setSecret(clientProps.getClientSecret());
                    config.setResponseType("code");
        
                    var client = new OidcClient(config);
                    client.setName(clientProps.getClientName());
        
                    String callbackUrl = casProperties.getServer().getPrefix() + "/login";
                    client.setCallbackUrl(callbackUrl);
        
                    LOGGER.debug("Registered delegated OIDC client [{}] with discovery [{}]", client.getName(), clientProps.getDiscoveryUri());
                    clients.add(client);
                }
        
                return clients;
            }
    
            @Override
            public Collection<BaseClient> rebuild() {
                LOGGER.debug("Rebuilding delegated clients...");
                return build();
            }
        };
    }
   
    /**
     * Makes the created OIDC clients available to CAS webflow.
     *
     * This bean connects the dynamically created OIDC clients into CAS's delegated authentication subsystem.
     * 
     * Whenever CAS asks for available identity providers, this class responds with the clients built earlier.
     * Explanation for CAS 7.1+:
     * - CAS has changed the internal management of clients
     * - The list of clients is now provided via DelegatedIdentityProviders
     * - An empty provider = no login options
     * 
     */    
    @Bean
    @Primary
    @RefreshScope
    public DelegatedIdentityProviders delegatedIdentityProviders(
        CasConfigurationProperties casProperties,
        DelegatedIdentityProviderFactory customDelegatedClientFactory
    ) {
        LOGGER.debug("Setting up custom delegated identity providers manually...");
    
        return new DelegatedIdentityProviders() {
            @Override
            public List<Client> findAllClients() {
                List<BaseClient> baseClients = new ArrayList<>(customDelegatedClientFactory.build());
                if (baseClients.isEmpty()) {
                    LOGGER.info("No delegated OIDC clients available! Check your cas.properties configuration.");
                }
                return new ArrayList<>(baseClients);
            }

            @Override
            public List<Client> findAllClients(Service service, WebContext context) {
                return findAllClients();
            }
    
            @Override
            public Optional<Client> findClient(String name) {
                return findAllClients()
                    .stream()
                    .filter(c -> c.getName().equalsIgnoreCase(name))
                    .findFirst();
            }
        };
    }
   
    // fixes No qualifying bean of type 'org.pac4j.core.client.Clients' available at logging out
    @Bean
    @RefreshScope
    public Clients builtClients(DelegatedIdentityProviders delegatedIdentityProviders) {
        var allClients = delegatedIdentityProviders.findAllClients();
        return new Clients(allClients);
    }

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
        String[] initialAdminUsernames = splitAndTrim(initialAadminUsernamesConfigString);
        String[] adminGroups = splitAndTrim(adminGroupsConfigString);

        return new CesDelegatedClientUserProfileProvisioner(userManager, initialAdminUsernames, adminGroups);
    }

    @Bean
    @RefreshScope
    public DelegatedAuthenticationPreProcessor delegatedAuthenticationPreProcessor(final CasConfigurationProperties casProperties) {

        UserManager userManager = getUserManager(casProperties);
        List<AttributeMapping> attributeMappings = AttributeMapping.fromPropertyString(attributesMappingsString);
        String[] allowedGroups = splitAndTrim(allowedGroupsConfigString);

        return new CesDelegatedAuthenticationPreProcessor(userManager, attributeMappings, allowedGroups);
    }
    
    private static UserManager getUserManager(CasConfigurationProperties casProperties) {
        var ldapList = casProperties.getAuthn().getLdap();
        if (ldapList == null || ldapList.isEmpty()) {
            throw new IllegalStateException("No LDAP configuration found");
        }
        LdapAuthenticationProperties ldapProperties = ldapList.get(0);
        PooledConnectionFactory connectionFactory = LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);
    
        return new UserManager(ldapProperties.getBaseDn(), new LdapOperationFactory(connectionFactory));
    }

    private static String[] splitAndTrim(String input) {
        if (StringUtils.isBlank(input)) {
            return new String[0];
        }

        String[] result = StringUtils.split(input, ",");

        return Arrays.stream(result).map(String::trim).toArray(String[]::new);
    }
}
