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
package de.triology.cas.services.oauth;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.support.oauth.OAuthConstants;
import org.jasig.cas.support.oauth.OAuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * This controller is called after successful authentication and
 * redirects user to the callback url of the OAuth application. A code is
 * added which is the service ticket retrieved from previous authentication.
 *
 * We skip the authorization of the user and redirect directly to the application.
 */
public final class CesOAuth20CallbackAuthorizeController extends AbstractController {

    private final Logger logger = LoggerFactory.getLogger(CesOAuth20CallbackAuthorizeController.class);

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        // get CAS ticket
        final String ticket = request.getParameter(OAuthConstants.TICKET);
        logger.debug("{} : {}", OAuthConstants.TICKET, ticket);

        // retrieve callback url from session
        final HttpSession session = request.getSession();
        String callbackUrl = (String) session.getAttribute(OAuthConstants.OAUTH20_CALLBACKURL);
        logger.debug("{} : {}", OAuthConstants.OAUTH20_CALLBACKURL, callbackUrl);
        session.removeAttribute(OAuthConstants.OAUTH20_CALLBACKURL);

        if (StringUtils.isBlank(callbackUrl)) {
            logger.error("{} is missing from the session and can not be retrieved.", OAuthConstants.OAUTH20_CALLBACKURL);
            return new ModelAndView(OAuthConstants.ERROR_VIEW);
        }
        // and state
        final String state = (String) session.getAttribute(OAuthConstants.OAUTH20_STATE);
        logger.debug("{} : {}", OAuthConstants.OAUTH20_STATE, state);
        session.removeAttribute(OAuthConstants.OAUTH20_STATE);

        // return callback url with code & state
        callbackUrl = OAuthUtils.addParameter(callbackUrl, OAuthConstants.CODE, ticket);
        if (state != null) {
            callbackUrl = OAuthUtils.addParameter(callbackUrl, OAuthConstants.STATE, state);
        }
        logger.debug("{} : {}", OAuthConstants.OAUTH20_CALLBACKURL, callbackUrl);

        final Map<String, Object> model = new HashMap<String, Object>();
        model.put("callbackUrl", callbackUrl);

        response.setHeader("Location", callbackUrl);
        return OAuthUtils.writeText(response, "", 303);
    }
}
