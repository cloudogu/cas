package de.triology.cas.oidc.beans;

import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.logout.slo.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.util.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesOAuthSingleLogoutServiceMessageHandler}.
 */
class CesOAuthSingleLogoutServiceMessageHandlerTests {

    private CesOAuthSingleLogoutServiceMessageHandler handler;

    @BeforeEach
    void setUp() {
        // Mock all constructor dependencies
        HttpClient httpClient = mock(HttpClient.class);
        SingleLogoutMessageCreator logoutMessageBuilder = mock(SingleLogoutMessageCreator.class);
        ServicesManager servicesManager = mock(ServicesManager.class);
        SingleLogoutServiceLogoutUrlBuilder logoutUrlBuilder = mock(SingleLogoutServiceLogoutUrlBuilder.class);
        AuthenticationServiceSelectionPlan selectionPlan = mock(AuthenticationServiceSelectionPlan.class);

        // Initialize the handler
        handler = new CesOAuthSingleLogoutServiceMessageHandler(httpClient, logoutMessageBuilder,
                servicesManager, logoutUrlBuilder, true, selectionPlan);
    }

    @Test
    void getOrder_ShouldReturnZero() {
        // when
        int order = handler.getOrder();

        // then
        assertEquals(0, order, "Handler order should be 0");
    }

    @Test
    void supportsInternal_ShouldReturnTrue_ForOAuthRegisteredService() {
        // given
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredService registeredService = new OAuthRegisteredService();
        SingleLogoutExecutionRequest context = mock(SingleLogoutExecutionRequest.class);

        // when
        boolean supported = handler.supportsInternal(service, registeredService, context);

        // then
        assertTrue(supported, "Supports should return true for OAuthRegisteredService");
    }

    @Test
    void supportsInternal_ShouldReturnFalse_ForOidcRegisteredService() {
        // given
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredService registeredService = new OidcRegisteredService();
        SingleLogoutExecutionRequest context = mock(SingleLogoutExecutionRequest.class);

        // when
        boolean supported = handler.supportsInternal(service, registeredService, context);

        // then
        assertFalse(supported, "Supports should return false for OidcRegisteredService");
    }

    @Test
    void supportsInternal_ShouldReturnFalse_ForNonOAuthService() {
        // given
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredService registeredService = mock(RegisteredService.class); // Just a plain RegisteredService
        SingleLogoutExecutionRequest context = mock(SingleLogoutExecutionRequest.class);

        // when
        boolean supported = handler.supportsInternal(service, registeredService, context);

        // then
        assertFalse(supported, "Supports should return false for non-OAuth service");
    }
}
