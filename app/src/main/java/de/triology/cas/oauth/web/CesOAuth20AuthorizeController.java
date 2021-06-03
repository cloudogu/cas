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
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This controller is in charge of responding to the authorize
 * call in OAuth protocol. It stores the callback url and redirects user to the
 * login page with the callback service.
 */
public final class CesOAuth20AuthorizeController extends AbstractController {

    private static Logger LOGGER = LoggerFactory.getLogger(CesOAuth20AuthorizeController.class);

    private final String loginUrl;

    private final ServicesManager servicesManager;

    public CesOAuth20AuthorizeController(final ServicesManager servicesManager, final String loginUrl) {
        this.servicesManager = servicesManager;
        this.loginUrl = loginUrl;
    }

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response) {

        final String clientId = request.getParameter(CesOAuthConstants.CLIENT_ID);
        LOGGER.debug("{} : {}", CesOAuthConstants.CLIENT_ID, clientId);

        final String redirectUri = request.getParameter(CesOAuthConstants.REDIRECT_URI);
        LOGGER.debug("{} : {}", CesOAuthConstants.REDIRECT_URI, redirectUri);

        final String state = request.getParameter(CesOAuthConstants.STATE);
        LOGGER.debug("{} : {}", CesOAuthConstants.STATE, state);

        // clientId is required
        if (StringUtils.isBlank(clientId)) {
            LOGGER.error("Missing {}", CesOAuthConstants.CLIENT_ID);
            return new ModelAndView(CesOAuthConstants.ERROR_VIEW);
        }
        // redirectUri is required
        if (StringUtils.isBlank(redirectUri)) {
            LOGGER.error("Missing {}", CesOAuthConstants.REDIRECT_URI);
            return new ModelAndView(CesOAuthConstants.ERROR_VIEW);
        }

        final CesOAuthRegisteredService service = CesOAuthUtils.getRegisteredOAuthService(this.servicesManager, clientId);
        if (service == null) {
            LOGGER.error("Unknown {} : {}", CesOAuthConstants.CLIENT_ID, clientId);
            return new ModelAndView(CesOAuthConstants.ERROR_VIEW);
        }

        final String serviceId = service.getServiceId();
        if (!redirectUri.matches(serviceId)) {
            LOGGER.error("Unsupported {} : {} for serviceId : {}", CesOAuthConstants.REDIRECT_URI, redirectUri, serviceId);
            return new ModelAndView(CesOAuthConstants.ERROR_VIEW);
        }

        // keep info in session
        final HttpSession session = request.getSession();
        session.setAttribute(CesOAuthConstants.OAUTH20_CALLBACKURL, redirectUri);
        session.setAttribute(CesOAuthConstants.OAUTH20_SERVICE_NAME, service.getName());
        session.setAttribute(CesOAuthConstants.OAUTH20_STATE, state);

        final String callbackAuthorizeUrl = request.getRequestURL().toString()
                .replace("/" + CesOAuthConstants.AUTHORIZE_URL, "/" + CesOAuthConstants.CALLBACK_AUTHORIZE_URL);
        LOGGER.debug("{} : {}", CesOAuthConstants.CALLBACK_AUTHORIZE_URL, callbackAuthorizeUrl);

        final String loginUrlWithService = CesOAuthUtils.addParameter(loginUrl, CesOAuthConstants.SERVICE,
                callbackAuthorizeUrl);
        LOGGER.debug("loginUrlWithService : {}", loginUrlWithService);
        return CesOAuthUtils.redirectTo(loginUrlWithService);
    }
}
