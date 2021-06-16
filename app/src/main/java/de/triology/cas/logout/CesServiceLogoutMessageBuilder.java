package de.triology.cas.logout;

import lombok.val;
import org.apereo.cas.logout.DefaultSingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.util.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CesServiceLogoutMessageBuilder extends DefaultSingleLogoutMessageCreator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CesServiceLogoutMessageBuilder.class);

    @Override
    public SingleLogoutMessage create(final SingleLogoutRequestContext request) {
        TicketGrantingTicket ticket = request.getExecutionRequest().getTicketGrantingTicket();
        RegisteredService service = request.getRegisteredService();
        String logoutRequest = "";
        LOGGER.debug("Generate logout request for: [{}] to [{}]", service.getName(), service.getLogoutUrl());

        if (service instanceof OAuthRegisteredService) {
            for (String childTicket : ticket.getDescendantTickets()) {
                if (childTicket.startsWith("AT-")) {
                    // childTicket starting with AT- is the OAuth access token
                    logoutRequest = String.format("%s", childTicket);
                }
            }

            val builder = SingleLogoutMessage.builder();
            if (request.getLogoutType() == RegisteredServiceLogoutType.FRONT_CHANNEL) {
                LOGGER.trace("Attempting to deflate the logout message [{}]", logoutRequest);
                return builder.payload(CompressionUtils.deflate(logoutRequest)).build();
            }
            return builder.payload(logoutRequest).build();
        } else {
            // default service message creator
            return super.create(request);
        }
    }
}
