package de.triology.cas.pm;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.audit.AuditActionResolvers;
import org.apereo.cas.audit.AuditResourceResolvers;
import org.apereo.cas.audit.AuditableActions;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.configuration.CasConfigurationProperties;

import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.pm.PasswordResetUrlBuilder;
import org.apereo.cas.pm.web.flow.actions.SendPasswordResetInstructionsAction;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.inspektr.audit.annotation.Audit;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;


/**
 * Extends the class {@link SendPasswordResetInstructionsAction}.
 *
 * In the original class, an error is thrown if no email address is found for the username entered.
 * This is also the case if the user does not exist in the system.
 *
 * In order to prevent the CAS from finding out whether a user exists in the system, the method responsible for this
 * has been adapted accordingly.
 */
@Slf4j
public class CesSendPasswordResetInstructionsAction extends SendPasswordResetInstructionsAction {

    public CesSendPasswordResetInstructionsAction(CasConfigurationProperties casProperties, CommunicationsManager communicationsManager, PasswordManagementService passwordManagementService, TicketRegistry ticketRegistry, TicketFactory ticketFactory, PrincipalResolver principalResolver, PasswordResetUrlBuilder passwordResetUrlBuilder) {
        super(casProperties, communicationsManager, passwordManagementService, ticketRegistry, ticketFactory, principalResolver, passwordResetUrlBuilder);
    }

    @Audit(action = AuditableActions.REQUEST_CHANGE_PASSWORD,
            principalResolverName = "REQUEST_CHANGE_PASSWORD_PRINCIPAL_RESOLVER",
            actionResolverName = AuditActionResolvers.REQUEST_CHANGE_PASSWORD_ACTION_RESOLVER,
            resourceResolverName = AuditResourceResolvers.REQUEST_CHANGE_PASSWORD_RESOURCE_RESOLVER)
    @Override
    protected Event doExecute(final RequestContext requestContext) throws Exception {
        communicationsManager.validate();
        if (!communicationsManager.isMailSenderDefined() && !communicationsManager.isSmsSenderDefined()) {
            return getErrorEvent("contact.failed", "Unable to send email as no mail sender is defined", requestContext);
        }

        val query = buildPasswordManagementQuery(requestContext);
        if (StringUtils.isBlank(query.getUsername())) {
            return getErrorEvent("username.required", "No username is provided", requestContext);
        }

        val email = passwordManagementService.findEmail(query);
        val phone = passwordManagementService.findPhone(query);
        if (StringUtils.isBlank(email) && StringUtils.isBlank(phone)) {
            LOGGER.warn("No recipient is provided with a valid email/phone");
            // In the original code, an error event is returned here.
            // However, no error should occur if no e-mail address is set, since it can otherwise be determined whether
            // a user exists in the system or not
            return success();
        }

        return super.doExecute(requestContext);
    }
}
