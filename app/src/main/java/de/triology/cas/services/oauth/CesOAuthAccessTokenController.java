package de.triology.cas.services.oauth;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.support.oauth.OAuthConstants;
import org.jasig.cas.support.oauth.OAuthUtils;
import org.jasig.cas.support.oauth.services.OAuthRegisteredService;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This controller returns an access token which is the CAS
 * granting ticket according to the service and code (service ticket) given.
 */
public final class CesOAuthAccessTokenController extends AbstractController {

    private static Logger LOGGER = LoggerFactory.getLogger(CesOAuthAccessTokenController.class);

    private final ServicesManager servicesManager;

    private final TicketRegistry ticketRegistry;

    private final long timeout;

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String TOKEN_TYPE = "token_type";
    private static final String TOKEN_EXPIRES = "expires_in";
    private static final String TOKEN_TYPE_VALUE = "Bearer";

    public CesOAuthAccessTokenController(final ServicesManager servicesManager, final TicketRegistry ticketRegistry,
                                         final long timeout) {
        this.servicesManager = servicesManager;
        this.ticketRegistry = ticketRegistry;
        this.timeout = timeout;
    }

    /**
     * gets parameters from a request and forms a ModelAndView from json Data
     *
     * @param request
     * @param response
     * @return
     */
    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response) {

        final String redirectUri = request.getParameter(OAuthConstants.REDIRECT_URI);
        LOGGER.debug("{} : {}", OAuthConstants.REDIRECT_URI, redirectUri);

        final String clientId = request.getParameter(OAuthConstants.CLIENT_ID);
        LOGGER.debug("{} : {}", OAuthConstants.CLIENT_ID, clientId);

        final String clientSecret = request.getParameter(OAuthConstants.CLIENT_SECRET);

        final String code = request.getParameter(OAuthConstants.CODE);
        LOGGER.debug("{} : {}", OAuthConstants.CODE, code);

        //verify all components of the request are valid
        if (!verifyAccessTokenRequest(redirectUri, clientId, clientSecret, code)) {
            return OAuthUtils.writeTextError(response, OAuthConstants.INVALID_REQUEST, 400);
        }

        final ServiceTicket serviceTicket = getServiceTicketFromTicketRegistry(code);
        if (serviceTicket == null) {
            OAuthUtils.writeTextError(response, OAuthConstants.INVALID_GRANT, 400);
        }

        JSONObject json = makeJsonResponse(serviceTicket);
        // Send response as json
        response.setContentType(CONTENT_TYPE_JSON);

        LOGGER.debug("{} : {}", "respond", json.toJSONString());
        return OAuthUtils.writeText(response, json.toJSONString(), 200);
    }

    /**
     * gets a ServiceTicket from the ticket Registry
     *
     * @param code
     * @return a ServiceTicket or null
     */
    private ServiceTicket getServiceTicketFromTicketRegistry(String code) {
        final ServiceTicket serviceTicket = (ServiceTicket) ticketRegistry.getTicket(code);
        // service ticket should be valid
        if (serviceTicket == null || serviceTicket.isExpired()) {
            LOGGER.error("Code expired : {}", code);
            return null; //
        }
        return serviceTicket;
    }

    /**
     * creates a json which can be sent as an response
     *
     * @param serviceTicket
     * @return json Object
     */
    private JSONObject makeJsonResponse(ServiceTicket serviceTicket) {
        JSONObject json = new JSONObject();

        final TicketGrantingTicket ticketGrantingTicket = serviceTicket.getGrantingTicket();
        // remove service ticket as it is not needed anymore
        ticketRegistry.deleteTicket(serviceTicket.getId());

        final int expires = (int) (timeout - (System.currentTimeMillis()
                - ticketGrantingTicket.getCreationTime()) / 1000);

        json.put(OAuthConstants.ACCESS_TOKEN, ticketGrantingTicket.getId());
        json.put(TOKEN_TYPE, TOKEN_TYPE_VALUE);
        json.put(TOKEN_EXPIRES, expires);

        return json;
    }

    /**
     * ensures that various parts of the AccessToken Request are present and valid.
     *
     * @param redirectUri
     * @param clientId
     * @param clientSecret
     * @param code
     * @return true if everything is valid
     */
    private boolean verifyAccessTokenRequest(final String redirectUri,
                                             final String clientId, final String clientSecret, final String code) {

        final OAuthRegisteredService service = OAuthUtils.getRegisteredOAuthService(this.servicesManager, clientId);

        return verifyClientId(clientId) &&
                verifyRedirctUir(redirectUri) &&
                verifyClientSecret(clientSecret) &&
                verifyCode(code) &&
                verifyHasRegisteredOAuthService(service, clientId) &&
                verifyRedirectUirMatchesServiceId(service, redirectUri) &&
                verifySecretHash(service, clientSecret);
    }

    private boolean verifyClientId(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            LOGGER.error("Missing {}", OAuthConstants.CLIENT_ID);
            return false;
        }
        return true;
    }

    private boolean verifyRedirctUir(String redirectUri) {
        if (StringUtils.isBlank(redirectUri)) {
            LOGGER.error("Missing {}", OAuthConstants.REDIRECT_URI);
            return false;
        }
        return true;
    }

    private boolean verifyClientSecret(String clientSecret) {
        if (StringUtils.isBlank(clientSecret)) {
            LOGGER.error("Missing {}", OAuthConstants.CLIENT_SECRET);
            return false;
        }
        return true;
    }

    private boolean verifyCode(String code) {
        if (StringUtils.isBlank(code)) {
            LOGGER.error("Missing {}", OAuthConstants.CODE);
            return false;
        }
        return true;
    }

    private boolean verifyHasRegisteredOAuthService(OAuthRegisteredService service, String clientId) {
        if (service == null) {
            LOGGER.error("Unknown {} : {}", OAuthConstants.CLIENT_ID, clientId);
            return false;
        }
        return true;
    }

    private boolean verifyRedirectUirMatchesServiceId(OAuthRegisteredService service, String redirectUri) {
        if (!redirectUri.matches(service.getServiceId())) {
            LOGGER.error("Unsupported {} : {} for serviceId : {}", OAuthConstants.REDIRECT_URI, redirectUri, service.getServiceId());
            return false;
        }
        return true;
    }

    private boolean verifySecretHash(OAuthRegisteredService service, String clientSecret) {
        // clientSecretHash is a hash from cas over the clientSecretHash, therefore we need to encrypt the clientSecretHash to perform the check
        String clientSecretHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(clientSecret);
        if (!StringUtils.equals(service.getClientSecret(), clientSecretHash)) {
            LOGGER.error("Wrong client secret for service {}", service);
            return false;
        }
        return true;
    }
}
