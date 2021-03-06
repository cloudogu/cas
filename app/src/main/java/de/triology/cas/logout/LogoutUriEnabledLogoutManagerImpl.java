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
package de.triology.cas.logout;

import de.triology.cas.oauth.CesOAuthConstants;
import de.triology.cas.oauth.logout.CesOAuthLogoutMessageBuilder;
import de.triology.cas.oauth.service.CesOAuthRegisteredService;
import de.triology.cas.services.dogu.LogoutUriEnabledRegexRegisteredService;
import org.apache.commons.codec.binary.Base64;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.authentication.principal.SingleLogoutService;
import org.jasig.cas.logout.LogoutMessageCreator;
import org.jasig.cas.logout.LogoutRequest;
import org.jasig.cas.logout.LogoutRequestStatus;
import org.jasig.cas.services.LogoutType;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.util.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * This logout manager handles the Single Log Out process.
 *
 * @author Jerome Leleu
 * @since 4.0.0
 */
public final class LogoutUriEnabledLogoutManagerImpl implements org.jasig.cas.logout.LogoutManager {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutUriEnabledLogoutManagerImpl.class);

    /**
     * ASCII character set.
     */
    private static final Charset ASCII = Charset.forName("ASCII");

    /**
     * The services manager.
     */
    @NotNull
    private final ServicesManager servicesManager;

    /**
     * An HTTP client.
     */
    @NotNull
    private final HttpClient httpClient;

    @NotNull
    private final LogoutMessageCreator logoutMessageBuilder;

    @NotNull
    private CesOAuthLogoutMessageBuilder oAuthlogoutMessageBuilder;

    /**
     * Whether single sign out is disabled or not.
     */
    private boolean disableSingleSignOut = false;

    /**
     * Build the logout manager.
     *
     * @param servicesManager      the services manager.
     * @param httpClient           an HTTP client.
     * @param logoutMessageBuilder the builder to construct logout messages.
     */
    public LogoutUriEnabledLogoutManagerImpl(final ServicesManager servicesManager, final HttpClient httpClient,
                                             final LogoutMessageCreator logoutMessageBuilder) {
        this.servicesManager = servicesManager;
        this.httpClient = httpClient;
        this.logoutMessageBuilder = logoutMessageBuilder;
        this.oAuthlogoutMessageBuilder = new CesOAuthLogoutMessageBuilder();
    }

    /**
     * Perform a back channel logout for a given ticket granting ticket and returns all the logout requests.
     *
     * @param ticket a given ticket granting ticket.
     * @return all logout requests.
     */
    @Override
    public List<LogoutRequest> performLogout(final TicketGrantingTicket ticket) {
        final Map<String, Service> services;
        // synchronize the retrieval of the services and their cleaning for the TGT
        // to avoid concurrent logout mess ups
        this.oAuthlogoutMessageBuilder.setTicket(ticket);
        synchronized (ticket) {
            services = ticket.getServices();
            ticket.removeAllServices();
        }
        ticket.markTicketExpired();

        final List<LogoutRequest> logoutRequests = new ArrayList<>();
        // if SLO is not disabled
        if (!disableSingleSignOut) {
            // through all services
            for (final String ticketId : services.keySet()) {
                final Service service = services.get(ticketId);
                // it's a SingleLogoutService, else ignore
                if (service instanceof SingleLogoutService) {
                    final SingleLogoutService singleLogoutService = (SingleLogoutService) service;
                    // the logout has not performed already
                    if (!singleLogoutService.isLoggedOutAlready()) {
                        final LogoutRequest logoutRequest = new LogoutRequest(ticketId, singleLogoutService);
                        // always add the logout request
                        logoutRequests.add(logoutRequest);
                        if (service.getId().contains(CesOAuthConstants.CALLBACK_AUTHORIZE_URL)) {
                            //For the callback authorize, logout every o auth service separately
                            for (RegisteredService aService : servicesManager.getAllServices()) {
                                if (aService instanceof CesOAuthRegisteredService) {
                                    final CesOAuthRegisteredService oAuthService = (CesOAuthRegisteredService) aService;
                                    // perform back channel logout
                                    performTypeDependentBackChannelLogout(singleLogoutService, logoutRequest, oAuthService);
                                }
                            }
                        } else {
                            final RegisteredService registeredService = servicesManager.findServiceBy(service);
                            // the service is no more defined, or the logout type is not defined or is back channel
                            if (registeredService == null || registeredService.getLogoutType() == null
                                    || registeredService.getLogoutType() == LogoutType.BACK_CHANNEL) {
                                performTypeDependentBackChannelLogout(singleLogoutService, logoutRequest, registeredService);
                            }
                        }
                    }
                }
            }
        }

        return logoutRequests;
    }

    void performTypeDependentBackChannelLogout(SingleLogoutService singleLogoutService, LogoutRequest logoutRequest, RegisteredService registeredService) {
        boolean successfulLogout;
        if (registeredService instanceof CesOAuthRegisteredService) {
            successfulLogout = performOAuthServiceLogout(logoutRequest, (CesOAuthRegisteredService) registeredService);
        } else if (registeredService instanceof LogoutUriEnabledRegexRegisteredService) {
            successfulLogout = performBackChannelLogout(logoutRequest, (LogoutUriEnabledRegexRegisteredService) registeredService);
        } else {
            successfulLogout = performBackChannelLogout(logoutRequest, null);
        }

        final LogoutRequestStatus logoutRequestStatus;
        if (successfulLogout) {
            logoutRequestStatus = LogoutRequestStatus.SUCCESS;
        } else {
            logoutRequestStatus = LogoutRequestStatus.FAILURE;
            LOGGER.warn("Logout message not sent to [{}]; Continuing processing...",
                    singleLogoutService.getId());
        }
        logoutRequest.setStatus(logoutRequestStatus);
    }


    /**
     * Log out of a service through back channel.
     *
     * @param request the logout request.
     * @return if the logout has been performed.
     */
    boolean performBackChannelLogout(final LogoutRequest request, LogoutUriEnabledRegexRegisteredService registeredService) {
        final String logoutRequest = this.logoutMessageBuilder.create(request);
        request.getService().setLoggedOutAlready(true);
        String originalUrl = request.getService().getOriginalUrl();

        LOGGER.debug("Sending logout request for: [{}]", request.getService().getId());
        if (registeredService != null && registeredService.getLogoutUri() != null) {
            //Extract the correct service name. Example Service name is "CesDoguServiceFactory redmine"
            String serviceName = registeredService.getName().split(" ")[1];
            String cesUrl = originalUrl.split(serviceName)[0];
            String logoutUrl = cesUrl + serviceName + registeredService.getLogoutUri().toString();
            LOGGER.debug("Found LogoutUriEnabledRegexRegisteredService; will use cas logout URL: " + logoutUrl);
            return this.sendMessageToEndPoint(logoutUrl, logoutRequest, true);
        } else {
            LOGGER.debug("Found normal service; will use originalUrl: " + originalUrl);
            return this.sendMessageToEndPoint(originalUrl, logoutRequest, true);
        }
    }

    /**
     * Log out of a service through back channel.
     *
     * @param request the logout request.
     * @return if the logout has been performed.
     */
    boolean performOAuthServiceLogout(final LogoutRequest request, CesOAuthRegisteredService registeredService) {
        final String logoutRequest = this.oAuthlogoutMessageBuilder.create(request);
        request.getService().setLoggedOutAlready(true);
        String originalUrl = request.getService().getOriginalUrl();

        //Extract the correct service name. Example Service name is "CesDoguServiceFactory redmine"
        String serviceName = registeredService.getName().split(" ")[1];
        String cesUrl = originalUrl.split("cas")[0];

        String logoutUrl = cesUrl + serviceName;
        if (registeredService.getLogoutUri() != null) {
            logoutUrl += registeredService.getLogoutUri().toString();
        }
        LOGGER.debug("Found CesOAuthRegisteredService; will use OAuth logout URL: " + logoutUrl);
        LOGGER.debug("Request " + logoutRequest);
        return sendMessageToEndPoint(logoutUrl, logoutRequest, true);
    }

    public boolean sendMessageToEndPoint(String logoutUrl, String logoutRequest, boolean async) {
        return this.httpClient.sendMessageToEndPoint(logoutUrl, logoutRequest, async);
    }

    /**
     * Create a logout message for front channel logout.
     *
     * @param logoutRequest the logout request.
     * @return a front SAML logout message.
     */
    public String createFrontChannelLogoutMessage(final LogoutRequest logoutRequest) {
        final String logoutMessage = this.logoutMessageBuilder.create(logoutRequest);
        final Deflater deflater = new Deflater();
        deflater.setInput(logoutMessage.getBytes(ASCII));
        deflater.finish();
        final byte[] buffer = new byte[logoutMessage.length()];
        final int resultSize = deflater.deflate(buffer);
        final byte[] output = new byte[resultSize];
        System.arraycopy(buffer, 0, output, 0, resultSize);
        return Base64.encodeBase64String(output);
    }

    /**
     * Set if the logout is disabled.
     *
     * @param disableSingleSignOut if the logout is disabled.
     */
    public void setDisableSingleSignOut(final boolean disableSingleSignOut) {
        this.disableSingleSignOut = disableSingleSignOut;
    }
}
