package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceMatchingStrategy;
import org.apereo.cas.util.LoggingUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CesRegisteredServiceMatchingStrategy implements RegisteredServiceMatchingStrategy {
    @Override
    public boolean matches(RegisteredService registeredService, String serviceId) {
        try {
            val thisUrl = removePortFromUrl(URLDecoder.decode(serviceId, StandardCharsets.UTF_8));
            val serviceUrl = removePortFromUrl(URLDecoder.decode(serviceId, StandardCharsets.UTF_8));

            LOGGER.debug("CesRegisteredServiceMatchingStrategy: Decoded urls and comparing [{}] with [{}]", thisUrl, serviceUrl);
            return thisUrl.equalsIgnoreCase(serviceUrl);
        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return false;
    }

    private String removePortFromUrl(String service) {
        return service.replaceFirst(":\\d+", "");
    }
}
