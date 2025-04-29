package de.triology.cas.pm;

import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PmConfigurationTests {

    private PmConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new PmConfiguration();
    }

    @Test
    void sendPasswordResetInstructionsAction_ShouldReturnCesSendPasswordResetInstructionsAction() {
        // Mocks
        var casProperties = mock(CasConfigurationProperties.class);
        var passwordManagementService = mock(PasswordManagementService.class);
        var ticketRegistry = mock(TicketRegistry.class);
        var principalResolver = mock(PrincipalResolver.class);
        var communicationsManager = mock(CommunicationsManager.class);
        var ticketFactory = mock(TicketFactory.class);
        var passwordResetUrlBuilder = mock(PasswordResetUrlBuilder.class);
        var authenticationSystemSupport = mock(AuthenticationSystemSupport.class);
        var multifactorAuthenticationProviderSelector = mock(MultifactorAuthenticationProviderSelector.class);
        var applicationContext = mock(ApplicationContext.class);

        // Call the method
        var action = configuration.sendPasswordResetInstructionsAction(
                casProperties,
                passwordManagementService,
                ticketRegistry,
                principalResolver,
                communicationsManager,
                ticketFactory,
                passwordResetUrlBuilder,
                authenticationSystemSupport,
                multifactorAuthenticationProviderSelector,
                applicationContext
        );

        // Assertions
        assertNotNull(action, "sendPasswordResetInstructionsAction() should not return null");
        assertTrue(action instanceof CesSendPasswordResetInstructionsAction, 
                   "Returned bean should be CesSendPasswordResetInstructionsAction");
    }
}
