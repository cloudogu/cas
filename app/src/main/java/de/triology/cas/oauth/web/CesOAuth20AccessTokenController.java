/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.triology.cas.oauth.web;

import de.triology.cas.oauth.CesOAuthConstants;
import de.triology.cas.oauth.CesOAuthUtils;
import de.triology.cas.oauth.service.CesOAuthRegisteredService;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.services.ServicesManager;
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
public final class CesOAuth20AccessTokenController extends AbstractController {

    private static Logger LOGGER = LoggerFactory.getLogger(CesOAuth20AccessTokenController.class);

    private final ServicesManager servicesManager;

    private final TicketRegistry ticketRegistry;

    private final long timeout;

    public CesOAuth20AccessTokenController(final ServicesManager servicesManager, final TicketRegistry ticketRegistry,
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

        final String redirectUri = request.getParameter(CesOAuthConstants.REDIRECT_URI);
        LOGGER.debug("{} : {}", CesOAuthConstants.REDIRECT_URI, redirectUri);

        final String clientId = request.getParameter(CesOAuthConstants.CLIENT_ID);
        LOGGER.debug("{} : {}", CesOAuthConstants.CLIENT_ID, clientId);

        final String clientSecret = request.getParameter(CesOAuthConstants.CLIENT_SECRET);

        final String code = request.getParameter(CesOAuthConstants.CODE);
        LOGGER.debug("{} : {}", CesOAuthConstants.CODE, code);

        //verify all components of the request are valid
        if (!verifyAccessTokenRequest(redirectUri, clientId, clientSecret, code)) {
            return CesOAuthUtils.writeTextError(response, CesOAuthConstants.INVALID_REQUEST, 400);
        }

        final ServiceTicket serviceTicket = getServiceTicketFromTicketRegistry(code);
        if (serviceTicket == null) {
            CesOAuthUtils.writeTextError(response, CesOAuthConstants.INVALID_GRANT, 400);
        }

        JSONObject json = makeJsonResponse(serviceTicket);
        // Send response as json
        response.setContentType(CesOAuthConstants.CONTENT_TYPE_JSON);

        LOGGER.debug("{} : {}", "respond", json.toJSONString());
        return CesOAuthUtils.writeText(response, json.toJSONString(), 200);
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

        json.put(CesOAuthConstants.ACCESS_TOKEN, ticketGrantingTicket.getId());
        json.put(CesOAuthConstants.TOKEN_TYPE, CesOAuthConstants.TOKEN_TYPE_VALUE);
        json.put(CesOAuthConstants.TOKEN_EXPIRES, expires);

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

        final CesOAuthRegisteredService service = CesOAuthUtils.getRegisteredOAuthService(this.servicesManager, clientId);

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
            LOGGER.error("Missing {}", CesOAuthConstants.CLIENT_ID);
            return false;
        }
        return true;
    }

    private boolean verifyRedirctUir(String redirectUri) {
        if (StringUtils.isBlank(redirectUri)) {
            LOGGER.error("Missing {}", CesOAuthConstants.REDIRECT_URI);
            return false;
        }
        return true;
    }

    private boolean verifyClientSecret(String clientSecret) {
        if (StringUtils.isBlank(clientSecret)) {
            LOGGER.error("Missing {}", CesOAuthConstants.CLIENT_SECRET);
            return false;
        }
        return true;
    }

    private boolean verifyCode(String code) {
        if (StringUtils.isBlank(code)) {
            LOGGER.error("Missing {}", CesOAuthConstants.CODE);
            return false;
        }
        return true;
    }

    private boolean verifyHasRegisteredOAuthService(CesOAuthRegisteredService service, String clientId) {
        if (service == null) {
            LOGGER.error("Unknown {} : {}", CesOAuthConstants.CLIENT_ID, clientId);
            return false;
        }
        return true;
    }

    private boolean verifyRedirectUirMatchesServiceId(CesOAuthRegisteredService service, String redirectUri) {
        if (!redirectUri.matches(service.getServiceId())) {
            LOGGER.error("Unsupported {} : {} for serviceId : {}", CesOAuthConstants.REDIRECT_URI, redirectUri, service.getServiceId());
            return false;
        }
        return true;
    }

    private boolean verifySecretHash(CesOAuthRegisteredService service, String clientSecret) {
        // clientSecretHash is a hash from cas over the clientSecretHash, therefore we need to encrypt the clientSecretHash to perform the check
        String clientSecretHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex(clientSecret);
        if (!StringUtils.equals(service.getClientSecret(), clientSecretHash)) {
            LOGGER.error("Wrong client secret for service {}", service);
            return false;
        }
        return true;
    }
}
