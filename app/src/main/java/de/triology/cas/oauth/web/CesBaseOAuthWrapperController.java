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

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.ticket.registry.TicketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

/**
 * This controller is the base controller for wrapping OAuth protocol in CAS.
 * It finds the right sub controller to call according to the url.
 */
public abstract class CesBaseOAuthWrapperController extends AbstractController {

    protected final Logger logger = LoggerFactory.getLogger(CesBaseOAuthWrapperController.class);

    @NotNull
    protected String loginUrl;

    @NotNull
    protected ServicesManager servicesManager;

    @NotNull
    protected TicketRegistry ticketRegistry;

    @NotNull
    protected long timeout;

    @Override
    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {

        final String method = getMethod(request);
        logger.debug("method : {}", method);
        return internalHandleRequest(method, request, response);
    }

    protected abstract ModelAndView internalHandleRequest(String method, HttpServletRequest request,
            HttpServletResponse response) throws Exception;

    /**
     * Return the method to call according to the url.
     *
     * @param request the incoming http request
     * @return the method to call according to the url
     */
    private String getMethod(final HttpServletRequest request) {
        String method = request.getRequestURI();
        if (method.contains("?")) {
            method = StringUtils.substringBefore(method, "?");
        }
        final int pos = method.lastIndexOf("/");
        if (pos >= 0) {
            method = method.substring(pos + 1);
        }
        return method;
    }

    public void setServicesManager(final ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    public void setTicketRegistry(final TicketRegistry ticketRegistry) {
        this.ticketRegistry = ticketRegistry;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }
}
