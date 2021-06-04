package de.triology.cas.oauth.web;

import de.triology.cas.oauth.CesOAuthConstants;
import de.triology.cas.oauth.service.CesOAuthRegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.ServiceTicket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CesOAuthAccessTokenControllerTest {

    private static final long TIMEOUT = 1000;

    private CesOAuth20AccessTokenController cesOAuthAccessTokenController;

    @Before
    public void setUp() {
        final ServicesManager servicesManager = mock(ServicesManager.class);
        final ServiceTicket serviceTicket = mock(ServiceTicket.class);
        final TicketGrantingTicket ticketGrantingTicket = mock(TicketGrantingTicket.class);
        final TicketRegistry ticketRegistry = mock(TicketRegistry.class);

        CesOAuthRegisteredService mockService = new CesOAuthRegisteredService();
        mockService.setClientId("123");
        mockService.setServiceId("https://local.cloudogu.com/portainer/API/.*");
        mockService.setClientSecret("bccfe0b37c0a147f5335243f11894faaeeaf67d02039fb74e42716d8b54b892e");
        when(servicesManager.getAllServices()).thenReturn(new HashSet<>(Collections.singletonList(mockService)));

        LocalDateTime date =
                Instant.ofEpochMilli(TIMEOUT).atZone(ZoneId.systemDefault()).toLocalDateTime();
        when(ticketGrantingTicket.getCreationTime()).thenReturn(ZonedDateTime.of(date, ZoneId.systemDefault()));
        when(ticketGrantingTicket.getId()).thenReturn("98765");

        when(serviceTicket.isExpired()).thenReturn(false);
        when(serviceTicket.getTicketGrantingTicket()).thenReturn(ticketGrantingTicket);
        when(ticketRegistry.getTicket(any())).thenReturn(serviceTicket);


        cesOAuthAccessTokenController = new CesOAuth20AccessTokenController(servicesManager, ticketRegistry, TIMEOUT);
    }

    @Test
    public void handleRequestInternalShouldFail() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(CesOAuthConstants.REDIRECT_URI)).thenReturn("https://local.cloudogu.com/portainer/callback"); //will be matched against serviceId
        when(request.getParameter(CesOAuthConstants.CLIENT_ID)).thenReturn("123");
        when(request.getParameter(CesOAuthConstants.CLIENT_SECRET)).thenReturn("secret_123");
        when(request.getParameter(CesOAuthConstants.CODE)).thenReturn("code");

        HttpServletResponse response = mock(org.eclipse.jetty.server.Response.class);
        doCallRealMethod().when(response).setStatus(any(Integer.class));
        doCallRealMethod().when(response).getStatus();
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter(), false));

        // a successful write operation returns null
        assertNull(cesOAuthAccessTokenController.handleRequestInternal(request, response));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void handleRequestInternal() throws IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(CesOAuthConstants.REDIRECT_URI)).thenReturn("https://local.cloudogu.com/portainer/API/test"); //will be matched against serviceId
        when(request.getParameter(CesOAuthConstants.CLIENT_ID)).thenReturn("123");
        when(request.getParameter(CesOAuthConstants.CLIENT_SECRET)).thenReturn("secret_123");
        when(request.getParameter(CesOAuthConstants.CODE)).thenReturn("code");

        HttpServletResponse response = mock(org.eclipse.jetty.server.Response.class);
        doCallRealMethod().when(response).setStatus(any(Integer.class));
        doCallRealMethod().when(response).getStatus();
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter(), false));

        // a successful write operation returns null
        assertNull(cesOAuthAccessTokenController.handleRequestInternal(request, response));
        assertEquals(200, response.getStatus());
    }
}