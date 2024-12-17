package de.triology.cas.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.authentication.principal.DefaultServiceMatchingStrategy;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.util.LoggingUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Matching strategy for comparing services. It generally behaves like the {@link DefaultServiceMatchingStrategy}
 * but omits service ports
 */
@Getter
@Slf4j
public class CesServiceMatchingStrategy implements ServiceMatchingStrategy {
    @Override
    public boolean matches(final Service service, final Service serviceToMatch) {
        try {
            val thisUrl = removeHashRoutingFromUrl(removePortFromUrl(URLDecoder.decode(service.getId(), StandardCharsets.UTF_8)));
            val serviceUrl = removePortFromUrl(URLDecoder.decode(serviceToMatch.getId(), StandardCharsets.UTF_8));

            LOGGER.debug("Decoded urls and comparing [{}] with [{}]", thisUrl, serviceUrl);
            return thisUrl.equalsIgnoreCase(serviceUrl);
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return false;
    }

    private String removePortFromUrl(String service) {
        return service.replaceFirst(":\\d+", "");
    }

    private String removeHashRoutingFromUrl(String service) {
        return service.split("#")[0];
    }
}
