package de.triology.cas.services;

import lombok.val;
import org.apereo.cas.authentication.principal.DefaultServiceMatchingStrategy;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Matching strategy for comparing services. It generally behaves like the {@link DefaultServiceMatchingStrategy}
 * but omits service ports
 */
public class CesServiceMatchingStrategy extends DefaultServiceMatchingStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(CesServiceMatchingStrategy.class);

    public CesServiceMatchingStrategy(ServicesManager servicesManager) {
        super(servicesManager);
    }

    @Override
    public boolean matches(final Service service, final Service serviceToMatch) {
        try {
            val thisUrl = removePortFromUrl(URLDecoder.decode(service.getId(), StandardCharsets.UTF_8.name()));
            val serviceUrl = removePortFromUrl(URLDecoder.decode(serviceToMatch.getId(), StandardCharsets.UTF_8.name()));

            LOG.debug("Decoded urls and comparing [{}] with [{}]", thisUrl, serviceUrl);
            return thisUrl.equalsIgnoreCase(serviceUrl);
        } catch (final Exception e) {
            LoggingUtils.error(LOG, e);
        }
        return false;
    }

    private String removePortFromUrl(String service) {
        return service.replaceFirst(":\\d+", "");
    }
}
