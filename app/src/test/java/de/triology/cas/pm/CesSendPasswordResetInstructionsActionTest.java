package de.triology.cas.pm;

import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.email.EmailProperties;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.ResetPasswordManagementProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.binding.message.MessageContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.apereo.cas.services.ServicesManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CesSendPasswordResetInstructionsActionTest {

    /**
     * Extension of the class {@link CesSendPasswordResetInstructionsAction} to be tested.
     * <p>
     * Since one method uses a static method that cannot be mocked with Mockito, this method is overwritten.
     */
    class CesSendPasswordResetInstructionsActionExtensionForUnitTest extends CesSendPasswordResetInstructionsAction {
        public CesSendPasswordResetInstructionsActionExtensionForUnitTest(CasConfigurationProperties casProperties, CommunicationsManager communicationsManager, PasswordManagementService passwordManagementService, TicketRegistry ticketRegistry, TicketFactory ticketFactory, PrincipalResolver principalResolver, PasswordResetUrlBuilder passwordResetUrlBuilder, MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector, AuthenticationSystemSupport authenticationSystemSupport, ServicesManager servicesManager) {
            super(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, multifactorAuthenticationProviderSelector, authenticationSystemSupport, servicesManager);
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
    private ServicesManager servicesManager;


    private CesSendPasswordResetInstructionsActionExtensionForUnitTest cesSendPasswordResetInstructionsAction;

    @BeforeEach
    public void setup() {
        cesSendPasswordResetInstructionsAction = new CesSendPasswordResetInstructionsActionExtensionForUnitTest(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, multifactorAuthenticationProviderSelector, authenticationSystemSupport, servicesManager);
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
        when(passwordManagementService.findEmails(any())).thenThrow(new Throwable("Test exception"));

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
        when(passwordManagementService.findEmails(any())).thenReturn(java.util.Set.of("mail@test.com"));
        when(passwordManagementService.findPhone(any())).thenReturn("01234/56789");
        when(requestContext.getFlowScope()).thenReturn(new LocalAttributeMap<>());
        AuthenticationProperties mockAuthProps = mock(AuthenticationProperties.class);
        when(casProperties.getAuthn()).thenReturn(mockAuthProps);
        PasswordManagementProperties mockPmProperties = mock(PasswordManagementProperties.class);
        when(mockAuthProps.getPm()).thenReturn(mockPmProperties);
        ResetPasswordManagementProperties mockResetProps = mock(ResetPasswordManagementProperties.class);
        when(mockPmProperties.getReset()).thenReturn(mockResetProps);
        when(mockResetProps.isMultifactorAuthenticationEnabled()).thenReturn(false);
        EmailProperties mockEmailProps = mock(EmailProperties.class);
        when(mockResetProps.getMail()).thenReturn(mockEmailProps);
        when(mockEmailProps.getAttributeName()).thenReturn(java.util.List.of("mail"));
        org.apereo.cas.configuration.model.support.sms.SmsProperties mockSmsProps =
                mock(org.apereo.cas.configuration.model.support.sms.SmsProperties.class);
        when(mockResetProps.getSms()).thenReturn(mockSmsProps);
        when(mockSmsProps.getAttributeName()).thenReturn(java.util.List.of("phone"));
        when(authenticationSystemSupport.getPrincipalResolver()).thenReturn(principalResolver);
        org.apereo.cas.authentication.principal.Principal mockPrincipal =
                mock(org.apereo.cas.authentication.principal.Principal.class);
        when(mockPrincipal.getAttributes()).thenReturn(java.util.Map.of());
        when(principalResolver.resolve(any())).thenReturn(mockPrincipal);
        org.springframework.webflow.definition.FlowDefinition mockFlowDefinition =
                mock(org.springframework.webflow.definition.FlowDefinition.class);
        when(requestContext.getActiveFlow()).thenReturn(mockFlowDefinition);
        org.springframework.context.ApplicationContext mockApplicationContext =
                mock(org.springframework.context.ApplicationContext.class);
        when(mockFlowDefinition.getApplicationContext()).thenReturn(mockApplicationContext);
        when(mockApplicationContext.getBeansOfType(org.apereo.cas.authentication.MultifactorAuthenticationProvider.class))
                .thenReturn(java.util.Map.of());
        MessageContext mockMsgCtx = mock(MessageContext.class);
        when(requestContext.getMessageContext()).thenReturn(mockMsgCtx);

        Event result = cesSendPasswordResetInstructionsAction.doExecuteInternal(requestContext);

        assertNotNull(result);
        assertEquals("error", result.getId());
    }
}
