package de.triology.cas.oidc.beans;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.logout.slo.DefaultSingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.UrlValidator;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Slf4j
public class CesSingleLogoutServiceLogoutUrlBuilder extends DefaultSingleLogoutServiceLogoutUrlBuilder {

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
        return registeredService.getFriendlyName().equalsIgnoreCase(CasRegisteredService.FRIENDLY_NAME) ||
                registeredService.getFriendlyName().equalsIgnoreCase(OAUTH_CLIENT_FRIENDLY_NAME);
    }
}
