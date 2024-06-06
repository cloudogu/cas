package de.triology.cas.pm;

import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.ResetPasswordManagementProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CesSendPasswordResetInstructionsActionTest {

    /**
     * Extension of the class {@link CesSendPasswordResetInstructionsAction} to be tested.
     * <p>
     * Since one method uses a static method that cannot be mocked with Mockito, this method is overwritten.
     */
    class CesSendPasswordResetInstructionsActionExtensionForUnitTest extends CesSendPasswordResetInstructionsAction {
        public CesSendPasswordResetInstructionsActionExtensionForUnitTest(CasConfigurationProperties casProperties, CommunicationsManager communicationsManager, PasswordManagementService passwordManagementService, TicketRegistry ticketRegistry, TicketFactory ticketFactory, PrincipalResolver principalResolver, PasswordResetUrlBuilder passwordResetUrlBuilder, MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector, AuthenticationSystemSupport authenticationSystemSupport, ApplicationContext applicationContext) {
            super(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, multifactorAuthenticationProviderSelector, authenticationSystemSupport, applicationContext);
        }

        @Override
        protected PasswordManagementQuery buildPasswordManagementQuery(final RequestContext requestContext) {
            return passwordManagementQuery;
        }
    }

    @Mock
    private CasConfigurationProperties casProperties;

    @Mock
    private CommunicationsManager communicationsManager;

    @Mock
    private PasswordManagementService passwordManagementService;

    @Mock
    private TicketRegistry ticketRegistry;

    @Mock
    private TicketFactory ticketFactory;

    @Mock
    private PrincipalResolver principalResolver;

    @Mock
    private RequestContext requestContext;

    @Mock
    private PasswordManagementQuery passwordManagementQuery;

    @Mock
    private PasswordResetUrlBuilder passwordResetUrlBuilder;

    @Mock
    private MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector;

    @Mock
    private AuthenticationSystemSupport authenticationSystemSupport;

    @Mock
    private ApplicationContext applicationContext;


    private CesSendPasswordResetInstructionsActionExtensionForUnitTest cesSendPasswordResetInstructionsAction;

    @Before
    public void setup() {
        cesSendPasswordResetInstructionsAction = new CesSendPasswordResetInstructionsActionExtensionForUnitTest(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, multifactorAuthenticationProviderSelector, authenticationSystemSupport, applicationContext);
    }

    @Test
    public void doExecuteThrowsNoErrorWhenNoEmailAddressCouldBeDetermined() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("Dustin");

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("success", result.getId());
    }

    @Test
    public void doExecuteThrowsErrorWhenMailSenderAndSmsSenderIsNotDefined() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(false);
        when(communicationsManager.isMailSenderDefined()).thenReturn(false);

        MessageContext mockMsgCtx = mock(MessageContext.class);
        when(requestContext.getMessageContext()).thenReturn(mockMsgCtx);

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("error", result.getId());
    }

    @Test
    public void doExecuteThrowsErrorWhenNoUsernameCouldBeDetermined() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);

        MessageContext mockMsgCtx = mock(MessageContext.class);
        when(requestContext.getMessageContext()).thenReturn(mockMsgCtx);

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("error", result.getId());
    }

    @Test
    public void doExecuteWithoutErrorWhenEmailWasNotFound() throws Throwable {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("Dustin");
        when(passwordManagementService.findEmail(any())).thenThrow(new Throwable("Test exception"));

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("success", result.getId());
    }

    @Test
    public void doExecuteWithoutErrorWhenPhoneWasNotFound() throws Throwable {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("Dustin");
        when(passwordManagementService.findPhone(any())).thenThrow(new Throwable("Test exception"));

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("success", result.getId());
    }

    @Test
    public void doExecuteSuperWithErrorWhenEmailOrPhoneWasFound() throws Throwable {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("Dustin");
        when(passwordManagementService.findEmail(any())).thenReturn("mail@test.com");
        when(passwordManagementService.findPhone(any())).thenReturn("01234/56789");
        when(requestContext.getFlowScope()).thenReturn(new LocalAttributeMap<>());
        AuthenticationProperties mockAuthProps = mock(AuthenticationProperties.class);
        when(casProperties.getAuthn()).thenReturn(mockAuthProps);
        PasswordManagementProperties mockPmProperties = mock(PasswordManagementProperties.class);
        when(mockAuthProps.getPm()).thenReturn(mockPmProperties);
        ResetPasswordManagementProperties mockResetProps = mock(ResetPasswordManagementProperties.class);
        when(mockPmProperties.getReset()).thenReturn(mockResetProps);
        when(mockResetProps.isMultifactorAuthenticationEnabled()).thenReturn(false);
        MessageContext mockMsgCtx = mock(MessageContext.class);
        when(requestContext.getMessageContext()).thenReturn(mockMsgCtx);

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("error", result.getId());
    }
}
