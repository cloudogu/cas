package de.triology.cas.oidc.beans;

import org.apereo.cas.logout.slo.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesOAuthSingleLogoutMessageCreator}.
 */
public class CesOAuthSingleLogoutMessageCreatorTests {

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
        OidcRegisteredService service = new OidcRegisteredService();
        service.setId(1);
        service.setClientId("testOAuthClient");
        service.setClientSecret("testClientSecret");
        service.setLogoutUrl("org/custom/logout");

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);

        when(contextMock.getRegisteredService()).thenReturn(service);
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
        OidcRegisteredService service = new OidcRegisteredService();
        service.setId(1);
        service.setClientId("testOAuthClient");
        service.setClientSecret("testClientSecret");
        service.setLogoutUrl("org/custom/logout");

        // given - mocks
        SingleLogoutRequestContext contextMock = mock(SingleLogoutRequestContext.class);
        SingleLogoutExecutionRequest singleLogoutExecutionRequestMock = mock(SingleLogoutExecutionRequest.class);

        when(contextMock.getRegisteredService()).thenReturn(service);
        when(contextMock.getExecutionRequest()).thenReturn(singleLogoutExecutionRequestMock);
        when(singleLogoutExecutionRequestMock.getTicketGrantingTicket()).thenReturn(tgtMock);

        // when
        SingleLogoutMessage logoutMessage = builder.create(contextMock);

        // then
        assertEquals("", logoutMessage.getPayload());
        validateMockitoUsage();
    }
}
