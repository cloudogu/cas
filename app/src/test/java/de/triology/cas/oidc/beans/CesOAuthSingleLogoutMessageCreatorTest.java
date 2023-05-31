package de.triology.cas.oidc.beans;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.CesServiceData;
import org.apereo.cas.logout.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.ticket.TicketGrantingTicket;
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
        CesOAuthSingleLogoutMessageCreator builder = new CesOAuthSingleLogoutMessageCreator();

        // given - data
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
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
    public void testCreate_WrongAtTicket() throws Exception {
        // given
        CesOAuthSingleLogoutMessageCreator builder = new CesOAuthSingleLogoutMessageCreator();

        // given - data
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
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
}