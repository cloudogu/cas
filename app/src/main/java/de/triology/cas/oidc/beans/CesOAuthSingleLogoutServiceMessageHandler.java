package de.triology.cas.oidc.beans;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.logout.SingleLogoutExecutionRequest;
import org.apereo.cas.logout.slo.BaseSingleLogoutServiceMessageHandler;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.util.http.HttpClient;

/**
 * The message handler for the OAuth protocol. This message handler takes action when a OAuth service should be
 * logged out of the CAS.
 */
@Slf4j
public class CesOAuthSingleLogoutServiceMessageHandler extends BaseSingleLogoutServiceMessageHandler {

    public CesOAuthSingleLogoutServiceMessageHandler(final HttpClient httpClient,
                                                     final SingleLogoutMessageCreator logoutMessageBuilder,
                                                     final ServicesManager servicesManager,
                                                     final SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder,
                                                     final boolean asynchronous,
                                                     final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies) {
        super(httpClient, logoutMessageBuilder, servicesManager, singleLogoutServiceLogoutUrlBuilder,
                asynchronous, authenticationRequestServiceSelectionStrategies);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected boolean supportsInternal(final WebApplicationService singleLogoutService, final RegisteredService registeredService,
                                       final SingleLogoutExecutionRequest context) {
        return registeredService instanceof OAuthRegisteredService && !(registeredService instanceof OidcRegisteredService);
    }
}
