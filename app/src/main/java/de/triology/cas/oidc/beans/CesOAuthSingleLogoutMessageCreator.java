package de.triology.cas.oidc.beans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.CompressionUtils;

/**
 * The message creator for the OAuth protocol. Generally OAuth does not support a logout. However the CES sends the
 * token to be invalidated to a configured logout url. This token represents the current access token gained
 * in the initial OAuth login flow.
 */
@Slf4j
@RequiredArgsConstructor
public class CesOAuthSingleLogoutMessageCreator implements SingleLogoutMessageCreator {

    private final TicketRegistry ticketRegistry;

    @Override
    public SingleLogoutMessage create(final SingleLogoutRequestContext request) {
        final TicketGrantingTicket tgt = request.getExecutionRequest().getTicketGrantingTicket();
        final RegisteredService service = request.getRegisteredService();
        LOGGER.debug("Generate oauth logout request for: [{}] to [{}]", service.getName(), request.getLogoutUrl());

        final String logoutRequest = getOauthTicketId(tgt);

        val builder = SingleLogoutMessage.builder();
        if (request.getLogoutType() == RegisteredServiceLogoutType.FRONT_CHANNEL) {
            LOGGER.trace("Attempting to deflate the logout message [{}]", logoutRequest);
            return builder.payload(CompressionUtils.deflate(logoutRequest)).build();
        }

        return builder.payload(logoutRequest).build();
    }

    private String getOauthTicketId(final TicketGrantingTicket tgt) {
        for (Ticket ticket : ticketRegistry.getTickets()) {
            if (ticket.getId().startsWith("AT-")
                    && ticket instanceof OAuth20AccessToken oauth
                    && oauth.getTicketGrantingTicket().getId().equals(tgt.getId())) {
                // ticket is oauth-ticket of this ticket-granting-ticket
                return String.format("%s", ticket.getId());
            }
        }

        LOGGER.error("could not find oauth-ticket to create logout-message");

        return "";
    }
}
