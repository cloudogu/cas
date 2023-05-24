package de.triology.cas.oidc.beans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.services.RegisteredServiceLogoutType;
import org.apereo.cas.util.CompressionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The message creator for the OAuth protocol. Generally OAuth does not support a logout. However the CES sends the
 * token to be invalidated to a configured logout url. This token represents the current access token gained
 * in the initial OAuth login flow.
 */
@Slf4j
@RequiredArgsConstructor
public class CesOAuthSingleLogoutMessageCreator implements SingleLogoutMessageCreator {
    private static final Logger LOG = LoggerFactory.getLogger(CesOAuthSingleLogoutMessageCreator.class);

    @Override
    public SingleLogoutMessage create(final SingleLogoutRequestContext request) {
        var ticket = request.getExecutionRequest().getTicketGrantingTicket();
        var service = request.getRegisteredService();
        var logoutRequest = "";
        LOG.debug("Generate oauth logout request for: [{}] to [{}]", service.getName(), request.getLogoutUrl());

        for (String childTicket : ticket.getDescendantTickets()) {
            if (childTicket.startsWith("AT-")) {
                // childTicket starting with AT- is the OAuth access token
                logoutRequest = String.format("%s", childTicket);
            }
        }

        val builder = SingleLogoutMessage.builder();
        if (request.getLogoutType() == RegisteredServiceLogoutType.FRONT_CHANNEL) {
            LOG.trace("Attempting to deflate the logout message [{}]", logoutRequest);
            return builder.payload(CompressionUtils.deflate(logoutRequest)).build();
        }
        return builder.payload(logoutRequest).build();
    }
}
