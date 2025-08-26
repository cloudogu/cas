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
import java.util.Objects;

/**
 * Matching strategy for comparing services. It generally behaves like the {@link DefaultServiceMatchingStrategy},
 * but omits service ports and hash fragments from the URL.
 */
@Getter
@Slf4j
public class CesServiceMatchingStrategy implements ServiceMatchingStrategy {

    @Override
    public boolean matches(final Service service, final Service serviceToMatch) {
        try {
            if (service == null || serviceToMatch == null) {
                return false;
            }
            LOGGER.info("service {} matchservice {}", service.getId(), serviceToMatch.getId());

                if (Objects.isNull(service) || Objects.isNull(serviceToMatch)) {
                    LOGGER.info("One of the services is null: [{}], [{}]", service, serviceToMatch);
                    return false;
                }

                val original = decodeUrl(service.getId());
                val candidate = decodeUrl(serviceToMatch.getId());

                val normalizedOriginal = normalizeServiceUrl(original);
                val normalizedCandidate = normalizeServiceUrl(candidate);

                LOGGER.info("Comparing normalized URLs: [{}] vs [{}]", normalizedOriginal, normalizedCandidate);
                return normalizedOriginal.equalsIgnoreCase(normalizedCandidate);

        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
            return false;
        }
    }

    private String decodeUrl(final String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    private String normalizeServiceUrl(final String url) {
        return removeHashFragment(removePortFromUrl(url));
    }

    private String removePortFromUrl(final String url) {
        return url.replaceFirst(":\\d+", "");
    }

    private String removeHashFragment(final String url) {
        val idx = url.indexOf('#');
        return idx >= 0 ? url.substring(0, idx) : url;
    }
}
