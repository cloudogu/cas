package de.triology.cas.pm;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.binding.message.MessageContext;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CesSendPasswordResetInstructionsActionTests {

    private CesSendPasswordResetInstructionsActionExtensionForTest action;
    private CommunicationsManager communicationsManager;
    private PasswordManagementService passwordManagementService;
    private RequestContext requestContext;
    private CasConfigurationProperties casProperties;
    private TicketRegistry ticketRegistry;
    private TicketFactory ticketFactory;
    private PrincipalResolver principalResolver;
    private PasswordResetUrlBuilder passwordResetUrlBuilder;
    private MultifactorAuthenticationProviderSelector mfaProviderSelector;
    private AuthenticationSystemSupport authenticationSystemSupport;
    private ApplicationContext applicationContext;
    private PasswordManagementQuery passwordManagementQuery;

    @BeforeEach
    void setUp() throws Exception {
        casProperties = mock(CasConfigurationProperties.class);
        communicationsManager = mock(CommunicationsManager.class);
        passwordManagementService = mock(PasswordManagementService.class);
        ticketRegistry = mock(TicketRegistry.class);
        ticketFactory = mock(TicketFactory.class);
        principalResolver = mock(PrincipalResolver.class);
        passwordResetUrlBuilder = mock(PasswordResetUrlBuilder.class);
        mfaProviderSelector = mock(MultifactorAuthenticationProviderSelector.class);
        authenticationSystemSupport = mock(AuthenticationSystemSupport.class);
        applicationContext = mock(ApplicationContext.class);
        passwordManagementQuery = mock(PasswordManagementQuery.class);
        requestContext = mock(RequestContext.class);

        action = new CesSendPasswordResetInstructionsActionExtensionForTest(
                casProperties,
                communicationsManager,
                passwordManagementService,
                ticketRegistry,
                ticketFactory,
                principalResolver,
                passwordResetUrlBuilder,
                mfaProviderSelector,
                authenticationSystemSupport,
                applicationContext
        );
    }

    @Test
    void shouldReturnError_WhenNoMailSenderDefined() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(false);
        when(communicationsManager.isSmsSenderDefined()).thenReturn(false);

        when(requestContext.getMessageContext()).thenReturn(mock(MessageContext.class));

        Event event = action.doExecuteInternal(requestContext);

        assertNotNull(event);
        assertEquals("error", event.getId());
    }
    
    @Test
    void shouldReturnSuccess_WhenNoEmailOrPhoneFound() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("testuser");
    
        try {
            when(passwordManagementService.findEmail(any())).thenReturn(null);
            when(passwordManagementService.findPhone(any())).thenReturn(null);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    
        when(requestContext.getMessageContext()).thenReturn(mock(MessageContext.class));
    
        Event event = action.doExecuteInternal(requestContext);
    
        assertNotNull(event);
        assertEquals("success", event.getId());
    }
    
    
    
    
    @Test
    void shouldReturnError_WhenUsernameIsBlank() throws Exception {
        when(communicationsManager.isMailSenderDefined()).thenReturn(true);
        when(passwordManagementQuery.getUsername()).thenReturn("");

        when(requestContext.getMessageContext()).thenReturn(mock(MessageContext.class));

        Event event = action.doExecuteInternal(requestContext);

        assertNotNull(event);
        assertEquals("error", event.getId());
    }

    /**
     * Simple static class to override the password management query building.
     */
    static class CesSendPasswordResetInstructionsActionExtensionForTest extends CesSendPasswordResetInstructionsAction {
        CesSendPasswordResetInstructionsActionTests parent;

        CesSendPasswordResetInstructionsActionExtensionForTest(
            CasConfigurationProperties casProperties,
            CommunicationsManager communicationsManager,
            PasswordManagementService passwordManagementService,
            TicketRegistry ticketRegistry,
            TicketFactory ticketFactory,
            PrincipalResolver principalResolver,
            PasswordResetUrlBuilder passwordResetUrlBuilder,
            MultifactorAuthenticationProviderSelector multifactorAuthenticationProviderSelector,
            AuthenticationSystemSupport authenticationSystemSupport,
            ApplicationContext applicationContext
        ) {
            super(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory,
                    principalResolver, passwordResetUrlBuilder, multifactorAuthenticationProviderSelector,
                    authenticationSystemSupport, applicationContext);
        }

        @Override
        protected PasswordManagementQuery buildPasswordManagementQuery(RequestContext requestContext) {
            // Always return the injected mock query
            return ((CesSendPasswordResetInstructionsActionTests) TestInstanceHolder.INSTANCE).passwordManagementQuery;
        }
    }

    private static final class TestInstanceHolder {
        static CesSendPasswordResetInstructionsActionTests INSTANCE;
    }

    {
        TestInstanceHolder.INSTANCE = this;
    }
}
