package de.triology.cas.oidc.beans;

import org.apereo.cas.CasProtocolConstants;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.RedirectionAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesOidcClientRedirectActionBuilder}.
 */
class CesOidcClientRedirectActionBuilderTests {

    private CesOidcClientRedirectActionBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new CesOidcClientRedirectActionBuilder();
    }

    @Test
    void build_ShouldCreateRedirectUrl_WithoutRenewOrGateway() {
        // given
        CasClient casClient = createCasClient("https://cas.example.org/login", false, false);
        WebContext webContext = createWebContext("https://service.example.org/callback");

        // when
        Optional<RedirectionAction> action = builder.build(casClient, webContext);

        // then
        assertTrue(action.isPresent(), "Redirection action should be present");
        assertInstanceOf(FoundAction.class, action.get(), "Action should be of type FoundAction");

        FoundAction foundAction = (FoundAction) action.get();
        String location = foundAction.getLocation();

        assertTrue(location.startsWith("https://cas.example.org/login?"), "Login URL should have query params");
        assertTrue(location.contains(CasProtocolConstants.PARAMETER_SERVICE + "="), "Service parameter should exist");
        assertFalse(location.contains(CasProtocolConstants.PARAMETER_RENEW), "Renew parameter should not exist");
        assertFalse(location.contains(CasProtocolConstants.PARAMETER_GATEWAY), "Gateway parameter should not exist");
    }

    @Test
    void build_ShouldIncludeRenew_WhenConfigured() {
        // given
        CasClient casClient = createCasClient("https://cas.example.org/login", true, false);
        WebContext webContext = createWebContext("https://service.example.org/callback");

        // when
        Optional<RedirectionAction> action = builder.build(casClient, webContext);

        // then
        assertTrue(action.isPresent(), "Redirection action should be present");

        FoundAction foundAction = (FoundAction) action.get();
        String location = foundAction.getLocation();

        assertTrue(location.contains(CasProtocolConstants.PARAMETER_RENEW + "=true"), "Renew=true should be included");
    }

    @Test
    void build_ShouldIncludeGateway_WhenConfigured() {
        // given
        CasClient casClient = createCasClient("https://cas.example.org/login", false, true);
        WebContext webContext = createWebContext("https://service.example.org/callback");

        // when
        Optional<RedirectionAction> action = builder.build(casClient, webContext);

        // then
        assertTrue(action.isPresent(), "Redirection action should be present");

        FoundAction foundAction = (FoundAction) action.get();
        String location = foundAction.getLocation();

        assertTrue(location.contains(CasProtocolConstants.PARAMETER_GATEWAY + "=true"), "Gateway=true should be included");
    }

    @Test
    void build_ShouldHandleLoginUrlWithExistingQueryParams() {
        // given
        CasClient casClient = createCasClient("https://cas.example.org/login?custom=param", false, false);
        WebContext webContext = createWebContext("https://service.example.org/callback");

        // when
        Optional<RedirectionAction> action = builder.build(casClient, webContext);

        // then
        assertTrue(action.isPresent(), "Redirection action should be present");

        FoundAction foundAction = (FoundAction) action.get();
        String location = foundAction.getLocation();

        // Because login URL already had a query param, next should use '&'
        assertTrue(location.contains("&" + CasProtocolConstants.PARAMETER_SERVICE + "="), "Service param should be appended correctly with &");
    }

    // Helper method to mock CasClient
    private CasClient createCasClient(String loginUrl, boolean renew, boolean gateway) {
        CasConfiguration config = mock(CasConfiguration.class);
        when(config.getLoginUrl()).thenReturn(loginUrl);
        when(config.isRenew()).thenReturn(renew);
        when(config.isGateway()).thenReturn(gateway);

        CasClient casClient = mock(CasClient.class);
        when(casClient.getConfiguration()).thenReturn(config);

        // Always return a fixed service URL for tests
        when(casClient.computeFinalCallbackUrl(any())).thenReturn("https://service.example.org/callback");
        return casClient;
    }

    // Helper method to mock WebContext
    private WebContext createWebContext(String fullUrl) {
        WebContext context = mock(WebContext.class);
        when(context.getFullRequestURL()).thenReturn(fullUrl);
        return context;
    }
}
