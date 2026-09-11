package de.triology.cas.oidc.beans;

import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceAccessStrategy;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CesSingleLogoutServiceLogoutUrlBuilder}.
 */
class CesSingleLogoutServiceLogoutUrlBuilderTest {

    private CesSingleLogoutServiceLogoutUrlBuilder builder;
    private Optional<jakarta.servlet.http.HttpServletRequest> httpRequest;

    @BeforeEach
    void setUp() {
        ServicesManager servicesManager = mock(ServicesManager.class);
        UrlValidator urlValidator = mock(UrlValidator.class);
        builder = new CesSingleLogoutServiceLogoutUrlBuilder(servicesManager, urlValidator);
        httpRequest = Optional.empty();
    }

    @Test
    void supports_ReturnsFalse_WhenRegisteredServiceIsNull() {
        WebApplicationService service = mock(WebApplicationService.class);

        assertFalse(builder.supports(null, service, httpRequest));
    }

    @Test
    void supports_ReturnsFalse_WhenSingleLogoutServiceIsNull() {
        RegisteredService registeredService = mock(RegisteredService.class);

        assertFalse(builder.supports(registeredService, null, httpRequest));
    }

    @Test
    void supports_ReturnsFalse_WhenServiceAccessNotAllowed() {
        RegisteredService registeredService = mock(RegisteredService.class);
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredServiceAccessStrategy accessStrategy = mock(RegisteredServiceAccessStrategy.class);
        when(registeredService.getAccessStrategy()).thenReturn(accessStrategy);
        when(accessStrategy.isServiceAccessAllowed(registeredService, service)).thenReturn(false);

        assertFalse(builder.supports(registeredService, service, httpRequest));
    }

    @Test
    void supports_ReturnsTrue_ForCasRegisteredServiceFriendlyName() {
        RegisteredService registeredService = mock(RegisteredService.class);
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredServiceAccessStrategy accessStrategy = mock(RegisteredServiceAccessStrategy.class);
        when(registeredService.getAccessStrategy()).thenReturn(accessStrategy);
        when(accessStrategy.isServiceAccessAllowed(registeredService, service)).thenReturn(true);
        when(registeredService.getFriendlyName()).thenReturn(CasRegisteredService.FRIENDLY_NAME.toUpperCase());

        assertTrue(builder.supports(registeredService, service, httpRequest));
    }

    @Test
    void supports_ReturnsTrue_ForOAuth2ClientFriendlyName() {
        RegisteredService registeredService = mock(RegisteredService.class);
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredServiceAccessStrategy accessStrategy = mock(RegisteredServiceAccessStrategy.class);
        when(registeredService.getAccessStrategy()).thenReturn(accessStrategy);
        when(accessStrategy.isServiceAccessAllowed(registeredService, service)).thenReturn(true);
        when(registeredService.getFriendlyName()).thenReturn("oauth2 client");

        assertTrue(builder.supports(registeredService, service, httpRequest));
    }

    @Test
    void supports_ReturnsFalse_ForUnrelatedFriendlyName() {
        RegisteredService registeredService = mock(RegisteredService.class);
        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredServiceAccessStrategy accessStrategy = mock(RegisteredServiceAccessStrategy.class);
        when(registeredService.getAccessStrategy()).thenReturn(accessStrategy);
        when(accessStrategy.isServiceAccessAllowed(registeredService, service)).thenReturn(true);
        when(registeredService.getFriendlyName()).thenReturn("Some Other Service");

        assertFalse(builder.supports(registeredService, service, httpRequest));
    }
}
