package de.triology.cas.logout;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.oidc.services.CesOIDCServiceFactory;
import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.logout.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesServiceLogoutMessageBuilder}.
 */
public class CesServiceLogoutMessageBuilderTest {

    @Test
    public void test_create_oauth_message_successfully() throws Exception {
        // given
        CesServiceLogoutMessageBuilder builder = new CesServiceLogoutMessageBuilder();

        // given - data
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
        Map<String, String> serviceAttributes = new HashMap<>();
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "testOAuthClient");
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "clientSecret");
        CesServiceData expectedData = new CesServiceData("testOAuthClient", factory, serviceAttributes);
        RegisteredService expectedService = factory.createNewService(1, "localhost", URI.create("org/custom/logout"), expectedData);
        List<String> descendantTickets = new ArrayList<>();
        descendantTickets.add("AT-1-3WoIpZxO-qzMl7R3N3cOlG0eh9VrY-dn");

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);
        TicketGrantingTicket tgtMock = mock(TicketGrantingTicket.class);

        when(contextMock.getRegisteredService()).thenReturn(expectedService);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);
        when(tgtMock.getDescendantTickets()).thenReturn(descendantTickets);

        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertEquals(descendantTickets.get(0), logoutMessage.getPayload());
        validateMockitoUsage();
    }

    @Test
    public void test_create_oauth_message_without_at() throws Exception {
        // given
        CesServiceLogoutMessageBuilder builder = new CesServiceLogoutMessageBuilder();

        // given - data
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
        Map<String, String> serviceAttributes = new HashMap<>();
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "testOAuthClient");
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "clientSecret");
        CesServiceData expectedData = new CesServiceData("testOAuthClient", factory, serviceAttributes);
        RegisteredService expectedService = factory.createNewService(1, "localhost", URI.create("org/custom/logout"), expectedData);
        Collection<String> descendantTickets = new ArrayList<>();

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);
        TicketGrantingTicket tgtMock = mock(TicketGrantingTicket.class);

        when(contextMock.getRegisteredService()).thenReturn(expectedService);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);
        when(tgtMock.getDescendantTickets()).thenReturn(descendantTickets);

        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertEquals("", logoutMessage.getPayload());
        validateMockitoUsage();
    }

    @Test
    public void test_create_service_message_successfully() throws CesServiceCreationException {
        // given
        CesServiceLogoutMessageBuilder builder = new CesServiceLogoutMessageBuilder();

        // given - data
        CesDoguServiceFactory factory = new CesDoguServiceFactory();
        CesServiceData expectedData = new CesServiceData("testDoguClient", factory);
        RegisteredService expectedService = factory.createNewService(1, "localhost", URI.create("org/custom/logout"), expectedData);
        String expectedServiceTicket = "ST-2-ViFoWViiZ2A0TMhoMhbvq2Ey7Pg-cas";
        String expectedPrincipal = "testUser";

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);
        TicketGrantingTicket tgtMock = mock(TicketGrantingTicket.class);
        Authentication authMock = mock(Authentication.class);
        Principal principalMock = mock(Principal.class);

        when(contextMock.getRegisteredService()).thenReturn(expectedService);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(contextMock.getTicketId()).thenReturn(expectedServiceTicket);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);
        when(tgtMock.getAuthentication()).thenReturn(authMock);
        when(authMock.getPrincipal()).thenReturn(principalMock);
        when(principalMock.getId()).thenReturn(expectedPrincipal);

        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertTrue(logoutMessage.getPayload().contains("samlp:LogoutRequest"));
        assertTrue(logoutMessage.getPayload().contains(expectedServiceTicket));
        assertTrue(logoutMessage.getPayload().contains(expectedPrincipal));
        validateMockitoUsage();
    }
}