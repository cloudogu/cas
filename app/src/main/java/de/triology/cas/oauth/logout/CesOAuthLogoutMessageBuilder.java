package de.triology.cas.oauth.logout;

import org.jasig.cas.logout.LogoutMessageCreator;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CesOAuthLogoutMessageBuilder implements LogoutMessageCreator {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CesOAuthLogoutMessageBuilder.class);

    private TicketGrantingTicket currentTicket;
    /**
     * The logout request template.
     */
    private static final String LOGOUT_REQUEST_TEMPLATE =
            "%s";

    public CesOAuthLogoutMessageBuilder(){
        this.currentTicket = null;
    }

    public void setTicket(TicketGrantingTicket tgt){
        this.currentTicket = tgt;
    }

    @Override
    public String create(final LogoutRequest request) {
        final String logoutRequest = String.format(LOGOUT_REQUEST_TEMPLATE, this.currentTicket != null ? this.currentTicket.getId(): "");

        LOGGER.debug("Generated logout message: [{}]", logoutRequest);
        return logoutRequest;
    }

}
