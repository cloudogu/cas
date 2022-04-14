package de.triology.cas.oidc.beans;

import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.logout.slo.DefaultSingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class CesSingleLogoutServiceLogoutUrlBuilder extends DefaultSingleLogoutServiceLogoutUrlBuilder {
    protected static final Logger LOG = LoggerFactory.getLogger(CesSingleLogoutServiceLogoutUrlBuilder.class);

    private static final String OAUTH_CLIENT_FRIENDLY_NAME = "OAuth2 Client";

    public CesSingleLogoutServiceLogoutUrlBuilder(ServicesManager servicesManager, UrlValidator urlValidator) {
        super(servicesManager, urlValidator);
    }

    @Override
    public boolean supports(final RegisteredService registeredService,
                            final WebApplicationService singleLogoutService,
                            final Optional<HttpServletRequest> httpRequest) {
        if (registeredService == null) return false;
        if (singleLogoutService == null) return false;
        if (!registeredService.getAccessStrategy().isServiceAccessAllowed()) return false;
        return registeredService.getFriendlyName().equalsIgnoreCase(RegexRegisteredService.FRIENDLY_NAME) ||
                registeredService.getFriendlyName().equalsIgnoreCase(OAUTH_CLIENT_FRIENDLY_NAME);
    }
}
