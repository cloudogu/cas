package de.triology.cas.logout;

import de.triology.cas.services.LogoutUriEnabledRegexRegisteredService;
import org.apache.commons.codec.binary.Base64;
import org.jasig.cas.authentication.principal.SingleLogoutService;
import org.jasig.cas.logout.LogoutMessageCreator;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.logout.LogoutRequestStatus;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.util.HttpClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

public class LogoutUriEnabledLogoutManagerImplTest {

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
        when(logoutManager.performBackChannelLogout(logoutRequest, (LogoutUriEnabledRegexRegisteredService) registeredService)).thenReturn(false);

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