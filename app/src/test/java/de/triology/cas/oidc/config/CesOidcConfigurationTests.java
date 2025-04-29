package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.delegation.CesDelegatedOidcClientsProperties;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.pac4j.client.DelegatedIdentityProviderFactory;
import org.apereo.cas.pac4j.client.DelegatedIdentityProviders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ConfigurableApplicationContext;
import java.lang.reflect.Field;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CesOidcConfigurationTests {

    private CesOidcConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CesOidcConfiguration();

        // private fields, so reflection needed otherwise production code gets polluted
        try {
            setField(configuration, "casServerPrefix", "https://cas.example.org");
            setField(configuration, "redirectUri", "");
        } catch (Exception e) {
            fail("Could not set private fields via reflection: " + e.getMessage(), e);
        }    
    }

    @Test
    void shouldReturnCesDelegatedOidcClientsProperties() {
        var properties = configuration.cesDelegatedOidcClientsProperties();
        assertNotNull(properties, "CesDelegatedOidcClientsProperties should not be null");
    }

    @Test
    void shouldBuildCustomDelegatedClientsFactory() {
        var casProperties = mock(CasConfigurationProperties.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var cache = mock(com.github.benmanes.caffeine.cache.Cache.class);

        var clientsProps = new CesDelegatedOidcClientsProperties(); // empty
        var factory = configuration.customDelegatedClientFactory(casProperties, cache, applicationContext, clientsProps);

        assertNotNull(factory);
        assertTrue(factory.build() instanceof Collection);
    }

    @Test
    void shouldReturnDelegatedIdentityProviders() {
        var casProperties = mock(CasConfigurationProperties.class);
        DelegatedIdentityProviderFactory factory = mock(DelegatedIdentityProviderFactory.class);
        when(factory.build()).thenReturn(Collections.emptyList());

        var providers = configuration.delegatedIdentityProviders(casProperties, factory);
        assertNotNull(providers);
        assertTrue(providers.findAllClients().isEmpty());
        assertTrue(providers.findClient("unknown").isEmpty());
    }

    @Test
    void shouldBuildClients() {
        var providers = mock(DelegatedIdentityProviders.class);
        when(providers.findAllClients()).thenReturn(Collections.emptyList());

        var clients = configuration.builtClients(providers);

        assertNotNull(clients);
        assertTrue(clients.getClients().isEmpty());
    }

    @Test
    void shouldCreateOidcCasClientRedirectActionBuilder() {
        var builder = configuration.oidcCasClientRedirectActionBuilder();
        assertNotNull(builder);
    }

    @Test
    void shouldCreateDelegatedAuthenticationClientLogoutAction() {
        var builtClients = mock(ObjectProvider.class);
        var sessionStore = mock(ObjectProvider.class);

        when(builtClients.getObject()).thenReturn(new Clients());
        when(sessionStore.getObject()).thenReturn(mock(org.pac4j.core.context.session.SessionStore.class));

        var action = configuration.delegatedAuthenticationClientLogoutAction(builtClients, sessionStore);
        assertNotNull(action);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
