package de.triology.cas.logout;

import lombok.val;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.UniqueTicketIdGenerator;
import org.apereo.cas.util.CompressionUtils;
import org.apereo.cas.util.DefaultUniqueTicketIdGenerator;
import org.apereo.cas.util.ISOStandardDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CesServiceLogoutMessageBuilder implements SingleLogoutMessageCreator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CesServiceLogoutMessageBuilder.class);

    /**
     * A ticket Id generator.
     */
    private static final UniqueTicketIdGenerator GENERATOR = new DefaultUniqueTicketIdGenerator(18);

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
        } else {
            // default service message creator
            logoutRequest = String.format("<samlp:LogoutRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" ID=\"%s\" Version=\"2.0\" "
                            + "IssueInstant=\"%s\"><saml:NameID xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">%s"
                            + "</saml:NameID><samlp:SessionIndex>%s</samlp:SessionIndex></samlp:LogoutRequest>",
                    GENERATOR.getNewTicketId("LR"),
                    new ISOStandardDateFormat().getCurrentDateAndTime(),
                    ticket.getAuthentication().getPrincipal().getId(),
                    request.getTicketId());
        }

        val builder = SingleLogoutMessage.builder();
        if (request.getLogoutType() == RegisteredServiceLogoutType.FRONT_CHANNEL) {
            LOGGER.trace("Attempting to deflate the logout message [{}]", logoutRequest);
            return builder.payload(CompressionUtils.deflate(logoutRequest)).build();
        }
        return builder.payload(logoutRequest).build();
    }
}
