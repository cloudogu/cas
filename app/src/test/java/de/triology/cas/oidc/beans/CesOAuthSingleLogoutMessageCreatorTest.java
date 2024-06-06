package de.triology.cas.oidc.beans;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.CesServiceData;
import org.apereo.cas.logout.slo.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.Test;

import java.net.URI;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesOAuthSingleLogoutMessageCreator}.
 */
public class CesOAuthSingleLogoutMessageCreatorTest {

    @Test
    public void testCreate_Successful() throws Exception {
        // given
        TicketRegistry ticketRegistryMock = mock(TicketRegistry.class);
        TicketGrantingTicket tgtMock = mock(TicketGrantingTicket.class);
        when(tgtMock.getId()).thenReturn("TGT");

        List<Ticket> tickets = new ArrayList<>();

        OAuth20AccessToken oauthTicket = mock(OAuth20AccessToken.class);
        when(oauthTicket.getId()).thenReturn("AT-1-3WoIpZxO-qzMl7R3N3cOlG0eh9VrY-dn");
        when(oauthTicket.getTicketGrantingTicket()).thenReturn(tgtMock);
        tickets.add(oauthTicket);
        doReturn(tickets).when(ticketRegistryMock).getTickets();

        CesOAuthSingleLogoutMessageCreator builder = new CesOAuthSingleLogoutMessageCreator(ticketRegistryMock);

        // given - data
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        Map<String, String> serviceAttributes = new HashMap<>();
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "testOAuthClient");
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "clientSecret");
        CesServiceData expectedData = new CesServiceData("testOAuthClient", factory, serviceAttributes);
        RegisteredService expectedService = factory.createNewService(1, "localhost", URI.create("org/custom/logout"), expectedData);

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);

        when(contextMock.getRegisteredService()).thenReturn(expectedService);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);


        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertEquals(tickets.get(0).getId(), logoutMessage.getPayload());
        validateMockitoUsage();
    }

    @Test
    public void testCreate_WrongAtTicket() throws Exception {
        // given
        TicketRegistry ticketRegistryMock = mock(TicketRegistry.class);
        TicketGrantingTicket tgtMock = mock(TicketGrantingTicket.class);
        when(tgtMock.getId()).thenReturn("TGT-1");

        TicketGrantingTicket tgtMock2 = mock(TicketGrantingTicket.class);
        when(tgtMock2.getId()).thenReturn("TGT-2");

        List<Ticket> tickets = new ArrayList<>();

        OAuth20AccessToken oauthTicket = mock(OAuth20AccessToken.class);
        when(oauthTicket.getId()).thenReturn("AT-1-3WoIpZxO-qzMl7R3N3cOlG0eh9VrY-dn");
        when(oauthTicket.getTicketGrantingTicket()).thenReturn(tgtMock2);
        tickets.add(oauthTicket);
        doReturn(tickets).when(ticketRegistryMock).getTickets();

        CesOAuthSingleLogoutMessageCreator builder = new CesOAuthSingleLogoutMessageCreator(ticketRegistryMock);

        // given - data
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        Map<String, String> serviceAttributes = new HashMap<>();
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "testOAuthClient");
        serviceAttributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "clientSecret");
        CesServiceData expectedData = new CesServiceData("testOAuthClient", factory, serviceAttributes);
        RegisteredService expectedService = factory.createNewService(1, "localhost", URI.create("org/custom/logout"), expectedData);

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);

        when(contextMock.getRegisteredService()).thenReturn(expectedService);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);

        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertEquals("", logoutMessage.getPayload());
        validateMockitoUsage();
    }
}
