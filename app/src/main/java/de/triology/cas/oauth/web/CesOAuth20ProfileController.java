package de.triology.cas.oauth.web;


        import java.util.Map;

        import javax.servlet.http.HttpServletRequest;
        import javax.servlet.http.HttpServletResponse;

        import de.triology.cas.oauth.CesOAuthConstants;
        import org.apache.commons.io.IOUtils;
        import org.apache.commons.lang.StringUtils;
        import org.jasig.cas.authentication.principal.Principal;
        import org.jasig.cas.ticket.TicketGrantingTicket;
        import org.jasig.cas.ticket.registry.TicketRegistry;
        import org.slf4j.Logger;
        import org.slf4j.LoggerFactory;
        import org.springframework.web.servlet.ModelAndView;
        import org.springframework.web.servlet.mvc.AbstractController;

        import com.fasterxml.jackson.core.JsonFactory;
        import com.fasterxml.jackson.core.JsonGenerator;
        import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This controller returns a profile for the authenticated user
 * (identifier + attributes), found with the access token (CAS granting
 * ticket).
 *
 * @author Jerome Leleu
 * @since 3.5.0
 */
public final class CesOAuth20ProfileController extends AbstractController {

    private static Logger LOGGER = LoggerFactory.getLogger(CesOAuth20ProfileController.class);

    private static final String ID = "id";

    private static final String ATTRIBUTES = "attributes";

    private final TicketRegistry ticketRegistry;

    private final JsonFactory jsonFactory = new JsonFactory(new ObjectMapper());

    public CesOAuth20ProfileController(final TicketRegistry ticketRegistry) {
        this.ticketRegistry = ticketRegistry;
    }

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {

        //final String accessToken = request.getParameter(CesOAuthConstants.ACCESS_TOKEN);
        final String authHeader = request.getHeader("authorization");
        LOGGER.debug("{} : {}", CesOAuthConstants.ACCESS_TOKEN, authHeader);
        String accessToken = authHeader.split(" ")[1];

        final JsonGenerator jsonGenerator = this.jsonFactory.createJsonGenerator(response.getWriter());

        try {
            response.setContentType("application/json");

            // accessToken is required
            if (StringUtils.isBlank(accessToken)) {
                LOGGER.error("Missing {}", CesOAuthConstants.ACCESS_TOKEN);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("error", CesOAuthConstants.MISSING_ACCESS_TOKEN);
                jsonGenerator.writeEndObject();
                return null;
            }
            // get ticket granting ticket
            final TicketGrantingTicket ticketGrantingTicket = (TicketGrantingTicket) this.ticketRegistry
                    .getTicket(accessToken);
            if (ticketGrantingTicket == null || ticketGrantingTicket.isExpired()) {
                LOGGER.error("expired accessToken : {}", accessToken);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("error", CesOAuthConstants.EXPIRED_ACCESS_TOKEN);
                jsonGenerator.writeEndObject();
                return null;
            }
            // generate profile : identifier + attributes
            final Principal principal = ticketGrantingTicket.getAuthentication().getPrincipal();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(ID, principal.getId());
            jsonGenerator.writeArrayFieldStart(ATTRIBUTES);
            final Map<String, Object> attributes = principal.getAttributes();
            for (final String key : attributes.keySet()) {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeObjectField(key, attributes.get(key));
                jsonGenerator.writeEndObject();
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            return null;
        } finally {
            IOUtils.closeQuietly(jsonGenerator);
            response.flushBuffer();
        }
    }
}
