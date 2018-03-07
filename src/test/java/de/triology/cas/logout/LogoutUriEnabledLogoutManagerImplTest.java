package de.triology.cas.logout;

import de.triology.cas.services.LogoutUriEnabledRegexRegisteredService;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.authentication.principal.SingleLogoutService;
import org.jasig.cas.logout.LogoutMessageCreator;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.logout.LogoutRequestStatus;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.util.HttpClient;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class LogoutUriEnabledLogoutManagerImplTest {

    @Test
    public void performLogout() {
        ServicesManager servicesManager = mock(ServicesManager.class);
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(servicesManager, mock(HttpClient.class), mock(LogoutMessageCreator.class));
        TicketGrantingTicket tgt = mock(TicketGrantingTicket.class);
        Map<String, Service> services = new HashMap<>();
        String serviceTicket = "ST-123";
        SingleLogoutService service = mock(SingleLogoutService.class);
        services.put(serviceTicket, service);

        when(tgt.getServices()).thenReturn(services);

        when(service.isLoggedOutAlready()).thenReturn(false);
        when(servicesManager.findServiceBy(service)).thenReturn(null);

        List<LogoutRequest> logoutRequests = logoutManager.performLogout(tgt);
        verify(tgt).removeAllServices();
        verify(tgt).markTicketExpired();
        assertEquals(serviceTicket, logoutRequests.get(0).getTicketId());
    }

    @Test
    public void performLogoutWithDisabledSSO() {
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), mock(HttpClient.class), mock(LogoutMessageCreator.class));
        logoutManager.setDisableSingleSignOut(true);
        TicketGrantingTicket tgt = mock(TicketGrantingTicket.class);
        List<LogoutRequest> logoutRequests = logoutManager.performLogout(tgt);
        assertTrue(logoutRequests.isEmpty());
    }

    @Test
    public void performBackChannelLogout () throws URISyntaxException {
        HttpClient httpClient = mock(HttpClient.class);
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), httpClient, mock(LogoutMessageCreator.class));

        SingleLogoutService sls = mock(SingleLogoutService.class);
        when(sls.getOriginalUrl()).thenReturn("fqdn/serviceName/foo");
        LogoutRequest request = new LogoutRequest("ST-123", sls);

        LogoutUriEnabledRegexRegisteredService registeredService = mock(LogoutUriEnabledRegexRegisteredService.class);
        when(registeredService.getName()).thenReturn("serviceName");
        when(registeredService.getLogoutUri()).thenReturn(new URI("/logoutUri"));
        logoutManager.performBackChannelLogout(request, registeredService);
        verify(httpClient).sendMessageToEndPoint("fqdn/serviceName/logoutUri",null,true);
    }

    @Test
    public void performSuccessfulTypeDependentBackChannelLogout() {
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), mock(HttpClient.class), mock(LogoutMessageCreator.class));
        SingleLogoutService singleLogoutService = mock(SingleLogoutService.class);
        when(singleLogoutService.getId()).thenReturn("0");
        LogoutRequest logoutRequest = new LogoutRequest("test", mock(SingleLogoutService.class));
        RegisteredService registeredService = new LogoutUriEnabledRegexRegisteredService();
        when(logoutManager.performBackChannelLogout(logoutRequest, (LogoutUriEnabledRegexRegisteredService) registeredService)).thenReturn(true);

        logoutManager.performTypeDependentBackChannelLogout(singleLogoutService, logoutRequest, registeredService);
        assertEquals(LogoutRequestStatus.SUCCESS, logoutRequest.getStatus());
    }

    @Test
    public void performFailingTypeDependentBackChannelLogout() {
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), mock(HttpClient.class), mock(LogoutMessageCreator.class));
        SingleLogoutService singleLogoutService = mock(SingleLogoutService.class);
        when(singleLogoutService.getId()).thenReturn("0");
        LogoutRequest logoutRequest = new LogoutRequest("test", mock(SingleLogoutService.class));
        RegisteredService registeredService = new LogoutUriEnabledRegexRegisteredService();

        logoutManager.performTypeDependentBackChannelLogout(singleLogoutService, logoutRequest, registeredService);
        assertEquals(LogoutRequestStatus.FAILURE, logoutRequest.getStatus());
    }

    @Test
    public void performFailingTypeDependentBackChannelLogoutWithoutLogoutUriEnabledRegexRegisteredService() {
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), mock(HttpClient.class), mock(LogoutMessageCreator.class));
        SingleLogoutService singleLogoutService = mock(SingleLogoutService.class);
        when(singleLogoutService.getId()).thenReturn("0");
        LogoutRequest logoutRequest = new LogoutRequest("test", mock(SingleLogoutService.class));
        RegisteredService registeredService = new RegisteredServiceImpl();

        logoutManager.performTypeDependentBackChannelLogout(singleLogoutService, logoutRequest, registeredService);
        assertEquals(LogoutRequestStatus.FAILURE, logoutRequest.getStatus());
    }

    @Test
    public void createFrontChannelLogoutMessage() {
        LogoutMessageCreator logoutMessageCreator = mock(LogoutMessageCreator.class);
        when(logoutMessageCreator.create(any())).thenReturn("Test");
        LogoutUriEnabledLogoutManagerImpl logoutManager = new LogoutUriEnabledLogoutManagerImpl(mock(ServicesManager.class), mock(HttpClient.class), logoutMessageCreator);
        SingleLogoutService service = mock(SingleLogoutService.class);
        LogoutRequest logoutRequest = new LogoutRequest("TestId", service);
        String logoutMessage = logoutManager.createFrontChannelLogoutMessage(logoutRequest);
        assertEquals("eJwLSQ==", logoutMessage);
    }
}