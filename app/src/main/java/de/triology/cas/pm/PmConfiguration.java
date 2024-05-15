package de.triology.cas.pm;

import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.pm.web.flow.actions.SendPasswordResetInstructionsAction;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Configuration(value = "PmConfiguration")
@ComponentScan("de.triology.cas.pm")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class PmConfiguration {

    @Bean(name = "sendPasswordResetInstructionsAction")
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SendPasswordResetInstructionsAction sendPasswordResetInstructionsAction(
            final CasConfigurationProperties casProperties,
            @Qualifier(PasswordManagementService.DEFAULT_BEAN_NAME)
            final PasswordManagementService passwordManagementService,
            @Qualifier(TicketRegistry.BEAN_NAME)
            final TicketRegistry ticketRegistry,
            @Qualifier(PrincipalResolver.BEAN_NAME_PRINCIPAL_RESOLVER)
            final PrincipalResolver defaultPrincipalResolver,
            @Qualifier(CommunicationsManager.BEAN_NAME)
            final CommunicationsManager communicationsManager,
            @Qualifier(TicketFactory.BEAN_NAME)
            final TicketFactory ticketFactory,
            @Qualifier(PasswordResetUrlBuilder.BEAN_NAME)
            final PasswordResetUrlBuilder passwordResetUrlBuilder,
            @Qualifier(AuthenticationSystemSupport.BEAN_NAME)
            final AuthenticationSystemSupport authenticationSystemSupport,
            final ApplicationContext applicationContext) {

        return new CesSendPasswordResetInstructionsAction(casProperties, communicationsManager,
                passwordManagementService, ticketRegistry, ticketFactory, defaultPrincipalResolver, passwordResetUrlBuilder, authenticationSystemSupport, applicationContext);
    }
}
