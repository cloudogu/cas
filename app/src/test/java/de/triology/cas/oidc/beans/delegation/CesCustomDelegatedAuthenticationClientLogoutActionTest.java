package de.triology.cas.oidc.beans.delegation;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.session.SessionStore;
import org.apereo.cas.web.support.WebUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CesCustomDelegatedAuthenticationClientLogoutAction}.
 */
class CesCustomDelegatedAuthenticationClientLogoutActionTest {

    @Test
    void doExecute_returnsNullWhenNoProfileIsFound() {
        Clients clients = new Clients();
        SessionStore sessionStore = mock(SessionStore.class);
        CesCustomDelegatedAuthenticationClientLogoutAction action =
                new CesCustomDelegatedAuthenticationClientLogoutAction(clients, sessionStore, "https://cas.example.org/logout");

        RequestContext requestContext = mock(RequestContext.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(() -> WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext)).thenReturn(request);
            webUtils.when(() -> WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext)).thenReturn(response);

            // doExecute() always returns null, so what matters is that it completes without throwing
            // and actually reaches both WebUtils calls (i.e. hits the no-profile branch, not an earlier failure).
            Event event = assertDoesNotThrow(() -> action.doExecute(requestContext));

            assertNull(event, "doExecute always returns null, regardless of outcome");
            webUtils.verify(() -> WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext));
            webUtils.verify(() -> WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext));
        }
    }

    @Test
    void doExecute_swallowsExceptionsAndReturnsNull() {
        Clients clients = new Clients();
        SessionStore sessionStore = mock(SessionStore.class);
        CesCustomDelegatedAuthenticationClientLogoutAction action =
                new CesCustomDelegatedAuthenticationClientLogoutAction(clients, sessionStore, "https://cas.example.org/logout");

        RequestContext requestContext = mock(RequestContext.class);

        try (MockedStatic<WebUtils> webUtils = mockStatic(WebUtils.class)) {
            webUtils.when(() -> WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext))
                    .thenThrow(new IllegalStateException("no external context available"));

            Event event = action.doExecute(requestContext);

            assertNull(event, "Exceptions during logout resolution must be swallowed, not propagated");
        }
    }
}
