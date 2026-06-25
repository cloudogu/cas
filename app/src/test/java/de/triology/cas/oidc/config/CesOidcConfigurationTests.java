package de.triology.cas.oidc.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.triology.cas.oidc.beans.delegation.CesDelegatedOidcClientProperties;
import de.triology.cas.oidc.beans.delegation.CesDelegatedOidcClientsProperties;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.CasServerProperties;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.pac4j.Pac4jDelegatedAuthenticationCoreProperties;
import org.apereo.cas.configuration.model.support.pac4j.Pac4jDelegatedAuthenticationProperties;
import org.apereo.cas.pac4j.client.DelegatedIdentityProviderFactory;
import org.apereo.cas.pac4j.client.DelegatedIdentityProviders;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.util.LdapUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ldaptive.PooledConnectionFactory;
import org.mockito.MockedStatic;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.oidc.client.OidcClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.execution.Action;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;

class CesOidcConfigurationTests {

    private CesOidcConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CesOidcConfiguration();
        try {
            setField(configuration, "casServerPrefix", "https://cas.cloudogu.org");
            setField(configuration, "redirectUri", "");
            setField(configuration, "attributesMappingsString", "email=mail");
            setField(configuration, "allowedGroupsConfigString", "users,admins");
            setField(configuration, "initialAadminUsernamesConfigString", "admin");
            setField(configuration, "adminGroupsConfigString", "admins");
        } catch (Exception e) {
            fail("Could not set private fields via reflection: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldReturnCesDelegatedOidcClientsProperties() {
        CesDelegatedOidcClientsProperties properties = configuration.cesDelegatedOidcClientsProperties();
        assertNotNull(properties, "CesDelegatedOidcClientsProperties should not be null");
    }

    @Test
    void shouldBuildCustomDelegatedClientsFactory() {
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);

        var clientsProps = new CesDelegatedOidcClientsProperties(); // empty
        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);

        assertNotNull(factory);
        assertNotNull(factory.build());
    }

    @Test
    void shouldReturnDelegatedIdentityProviders() {
        BaseClient client = delegatedClient("oidc-client");
        DelegatedIdentityProviders providers = delegatedIdentityProvidersFor(client);
        var service = mock(Service.class);
        var webContext = mock(WebContext.class);

        assertNotNull(providers);
        assertEquals(java.util.List.of(client), providers.findAllClients(service, webContext));
        assertEquals(java.util.List.of(client), providers.findAllClients(webContext));
        assertSame(client, providers.findClient("OIDC-CLIENT", webContext).orElseThrow());
        assertTrue(providers.findClient("unknown", webContext).isEmpty());
    }

    @Test
    void shouldBuildClients() {
        BaseClient client = delegatedClient("oidc-client");
        DelegatedIdentityProviders providers = delegatedIdentityProvidersFor(client);

        Clients clients = configuration.builtClients(providers);

        assertNotNull(clients);
        assertEquals(java.util.List.of(client), clients.getClients());
    }

    @Test
    void shouldCreateOidcCasClientRedirectActionBuilder() {
        OAuth20CasClientRedirectActionBuilder builder = configuration.oidcCasClientRedirectActionBuilder();
        assertNotNull(builder);
    }

    @Test
    void shouldCreateDelegatedAuthenticationClientLogoutAction() {
        var builtClients = mock(ObjectProvider.class);
        var sessionStore = mock(ObjectProvider.class);

        when(builtClients.getObject()).thenReturn(new Clients());
        when(sessionStore.getObject()).thenReturn(mock(org.pac4j.core.context.session.SessionStore.class));

        Action action = configuration.delegatedAuthenticationClientLogoutAction(builtClients, sessionStore);
        assertNotNull(action);
    }

    @Test
    void shouldCreateClientUserProfileProvisioner() {
        var casProperties = mock(CasConfigurationProperties.class);
        var authnProps = mock(org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties.class);
        var ldapProps = mock(org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties.class);
    
        when(casProperties.getAuthn()).thenReturn(authnProps);
        when(authnProps.getLdap()).thenReturn(java.util.List.of(ldapProps));
    
        when(ldapProps.getBaseDn()).thenReturn("dc=example,dc=org");
        when(ldapProps.getLdapUrl()).thenReturn("ldap://localhost");
    
        try (MockedStatic<LdapUtils> mocked = mockStatic(LdapUtils.class)) {
            var mockFactory = mock(org.ldaptive.PooledConnectionFactory.class);
            mocked.when(() -> LdapUtils.newLdaptivePooledConnectionFactory(any())).thenReturn(mockFactory);

            DelegatedClientUserProfileProvisioner provisioner = configuration.clientUserProfileProvisioner(casProperties);
            assertNotNull(provisioner);
        }
    }   
    
    @Test
    void shouldSplitAndTrimCorrectly() throws Exception {
        var result = (String[]) callSplitAndTrim(" group1 ,group2 , group3 ");
        assertArrayEquals(new String[]{"group1", "group2", "group3"}, result);
    }
    
    @Test
    void shouldHandleBlankInputInSplitAndTrim() throws Exception {
        var result = (String[]) callSplitAndTrim("");
        assertEquals(0, result.length);
    }

    @Test
    void shouldUseFallbackRedirectUriWhenEmpty() {
        var configuration = new CesOidcConfiguration();

        try {
            setField(configuration, "casServerPrefix", "https://cas.example.org");
            setField(configuration, "redirectUri", "");
        } catch (Exception e) {
            fail("Could not set private fields via reflection: " + e.getMessage(), e);
        }

        var builtClients = mock(ObjectProvider.class);
        var sessionStore = mock(ObjectProvider.class);
    
        when(builtClients.getObject()).thenReturn(new Clients());
        when(sessionStore.getObject()).thenReturn(mock(org.pac4j.core.context.session.SessionStore.class));

        Action action = configuration.delegatedAuthenticationClientLogoutAction(builtClients, sessionStore);
        assertNotNull(action);
    }
    
    @Test
    void shouldLogWhenNoDelegatedClientsAvailable() {
        var configuration = new CesOidcConfiguration();
        var casProperties = mock(CasConfigurationProperties.class);
        var factory = mock(DelegatedIdentityProviderFactory.class);
    
        when(factory.build()).thenReturn(Collections.emptyList());

        DelegatedIdentityProviders providers = configuration.delegatedIdentityProviders(casProperties, factory);
        List<? extends Client> clients = providers.findAllClients(mock(WebContext.class));
    
        assertTrue(clients.isEmpty(), "Clients should be empty, triggering log output");
    }
    
    @Test
    void shouldRebuildDelegatedClients() {
        var configuration = new CesOidcConfiguration();
        var casProperties = mock(CasConfigurationProperties.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
    
        var clientsProps = new CesDelegatedOidcClientsProperties(); // leer
        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);

        List<BaseClient> rebuiltClients = factory.rebuild();
        assertNotNull(rebuiltClients);
    }
    
    @Test
    void shouldSkipInvalidOidcClientProperties() {
        var configuration = new CesOidcConfiguration();
        var casProperties = mock(CasConfigurationProperties.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
    
        var invalidClientProps = new CesDelegatedOidcClientProperties();
        invalidClientProps.setClientName("invalid-client"); // nur Name, Rest fehlt
    
        var clientsProps = new CesDelegatedOidcClientsProperties();
        clientsProps.setClients(java.util.List.of(invalidClientProps));

        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);
        List<BaseClient> clients = factory.build();
    
        assertTrue(clients.isEmpty(), "Invalid clients should be skipped");
    }
    
    private Object callSplitAndTrim(String input) throws Exception {
        Method method = CesOidcConfiguration.class.getDeclaredMethod("splitAndTrim", String.class);
        method.setAccessible(true);
        return method.invoke(null, input);
    }
    
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private BaseClient delegatedClient(String name) {
        var client = mock(BaseClient.class);
        when(client.getName()).thenReturn(name);
        return client;
    }

    private DelegatedIdentityProviders delegatedIdentityProvidersFor(BaseClient... clients) {
        var casProperties = mock(CasConfigurationProperties.class);
        DelegatedIdentityProviderFactory factory = mock(DelegatedIdentityProviderFactory.class);
        when(factory.build()).thenReturn(java.util.List.of(clients));

        return configuration.delegatedIdentityProviders(casProperties, factory);
    }

    @Test
    void shouldBuildValidOidcClientFromProperties() {
        //var configuration = new CesOidcConfiguration(); // wird schon von @BeforeEach bereitgestellt
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);
    
        var clientProps = new CesDelegatedOidcClientProperties();
        clientProps.setClientId("client-id");
        clientProps.setClientName("test-client");        
        clientProps.setClientSecret("client-secret");
        clientProps.setDiscoveryUri("https://issuer/.well-known/openid-configuration");
        clientProps.setClientAuthenticationMethod("client_secret_basic");
        clientProps.setPreferredJwsAlgorithm("RS256");
    
        var clientsProps = new CesDelegatedOidcClientsProperties();
        clientsProps.setClients(java.util.List.of(clientProps));
    
        var serverProps = mock(CasServerProperties.class);
        when(serverProps.getPrefix()).thenReturn("https://cas.example.org");
        when(casProperties.getServer()).thenReturn(serverProps);

        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);
        List<BaseClient> clients = factory.build();
    
        assertEquals(1, clients.size());
        var client = (org.pac4j.oidc.client.OidcClient) clients.iterator().next();
        assertEquals("test-client", client.getName());
        assertEquals("https://cas.example.org/login", client.getCallbackUrl());
    }

    @Test
    void shouldProvidePac4jDelegatedClientFactoryCache() {
        var casProperties = mock(CasConfigurationProperties.class);
        var authnProps = mock(AuthenticationProperties.class);
        var pac4jProps = mock(Pac4jDelegatedAuthenticationProperties.class);
        var coreProps = mock(Pac4jDelegatedAuthenticationCoreProperties.class);

        when(casProperties.getAuthn()).thenReturn(authnProps);
        when(authnProps.getPac4j()).thenReturn(pac4jProps);
        when(pac4jProps.getCore()).thenReturn(coreProps);
        when(coreProps.getCacheSize()).thenReturn(100L);
        when(coreProps.getCacheDuration()).thenReturn("PT8H");

        Cache<String, List<BaseClient>> cache = configuration.pac4jDelegatedClientFactoryCache(casProperties);

        assertNotNull(cache);
        // Prove the configured Caffeine cache is functional, not just non-null.
        cache.put("key", java.util.List.of());
        assertNotNull(cache.getIfPresent("key"));
    }

    @Test
    void shouldStoreAndRetrieveClientsViaFactoryCache() {
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        com.github.benmanes.caffeine.cache.Cache<String, java.util.List<BaseClient>> cache =
                Caffeine.newBuilder().build();

        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(
                casProperties, cache, applicationContext, new CesDelegatedOidcClientsProperties());

        // Empty cache -> the null branch must yield an empty (non-null) list.
        assertTrue(factory.retrieve("missing").isEmpty());

        var client = mock(BaseClient.class);
        factory.store("key", java.util.List.of(client));
        assertEquals(java.util.List.of(client), factory.retrieve("key"));
    }

    @Test
    void shouldBuildClientsFromExplicitPropertiesViaBuildFrom() throws Exception {
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);

        var clientProps = new CesDelegatedOidcClientProperties();
        clientProps.setClientId("client-id");
        clientProps.setClientName("buildfrom-client");
        clientProps.setClientSecret("client-secret");
        clientProps.setDiscoveryUri("https://issuer/.well-known/openid-configuration");
        clientProps.setClientAuthenticationMethod("client_secret_basic");
        clientProps.setPreferredJwsAlgorithm("RS256");

        var clientsProps = new CesDelegatedOidcClientsProperties();
        clientsProps.setClients(java.util.List.of(clientProps));

        // buildFrom must use the passed-in properties (not the captured casProperties) for the callback URL.
        var serverProps = mock(CasServerProperties.class);
        when(serverProps.getPrefix()).thenReturn("https://other.example.org");
        var explicitProperties = mock(CasConfigurationProperties.class);
        when(explicitProperties.getServer()).thenReturn(serverProps);

        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);
        List<BaseClient> clients = factory.buildFrom(explicitProperties);

        assertEquals(1, clients.size());
        var client = (org.pac4j.oidc.client.OidcClient) clients.getFirst();
        assertEquals("buildfrom-client", client.getName());
        assertEquals("https://other.example.org/login", client.getCallbackUrl());
    }

    @Test
    void shouldDestroyCachedClients() throws Exception {
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);

        DelegatedIdentityProviderFactory factory = configuration.customDelegatedClientFactory(
                casProperties, cache, applicationContext, new CesDelegatedOidcClientsProperties());

        assertNotNull(factory.build());
        factory.destroy();
        // After destroy() the internal cache is reset and build() must repopulate it without error.
        assertNotNull(factory.build());
    }
}
