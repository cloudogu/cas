package de.triology.cas.pm;

import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
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
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CesSendPasswordResetInstructionsActionTest {

    /**
     * Extension of the class {@link CesSendPasswordResetInstructionsAction} to be tested.
     * <p>
     * Since one method uses a static method that cannot be mocked with Mockito, this method is overwritten.
     */
    class CesSendPasswordResetInstructionsActionExtensionForUnitTest extends CesSendPasswordResetInstructionsAction {
        public CesSendPasswordResetInstructionsActionExtensionForUnitTest(CasConfigurationProperties casProperties, CommunicationsManager communicationsManager, PasswordManagementService passwordManagementService, TicketRegistry ticketRegistry, TicketFactory ticketFactory, PrincipalResolver principalResolver, PasswordResetUrlBuilder passwordResetUrlBuilder, AuthenticationSystemSupport authenticationSystemSupport, ApplicationContext applicationContext) {
            super(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, authenticationSystemSupport, applicationContext);
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
    private AuthenticationSystemSupport authenticationSystemSupport;

    @Mock
    private ApplicationContext applicationContext;


    private CesSendPasswordResetInstructionsActionExtensionForUnitTest cesSendPasswordResetInstructionsAction;

    @Before
    public void setup() {
        cesSendPasswordResetInstructionsAction = new CesSendPasswordResetInstructionsActionExtensionForUnitTest(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder, authenticationSystemSupport, applicationContext);
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
}
