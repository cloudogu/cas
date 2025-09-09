package de.triology.cas.principal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.Principal;

import lombok.extern.slf4j.Slf4j;

/**
 * Principal factory that rewrites the principal ID to the first non-empty
 * value found among a list of attribute candidates (e.g., preferred_username, displayName).
 *
 * This runs everywhere CAS calls PrincipalFactory#createPrincipal(...),
 * including delegated/pac4j, so services will see the chosen value as the username.
 */
@Slf4j
public class AttributeSelectingPrincipalFactory extends DefaultPrincipalFactory {
    private final List<String> idAttributeCandidates;

    public AttributeSelectingPrincipalFactory(final String... idAttributeCandidates) {
        this.idAttributeCandidates = Arrays.asList(idAttributeCandidates);
    }

    private static boolean looksLikeUuid(final String s) {
        return s != null && s.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    @Override
    public Principal createPrincipal(final String originalId,
                                     final Map<String, List<Object>> attributes) {
        LOGGER.debug("Candidate keys: {}", idAttributeCandidates);
        LOGGER.debug("Available attribute keys: {}", attributes != null ? attributes.keySet() : "null");

        String chosen = null;
        if (attributes != null) {
            for (String key : idAttributeCandidates) {
                Object raw = attributes.get(key);
                String value = null;
                if (raw instanceof List<?> l && !l.isEmpty() && l.get(0) != null) value = String.valueOf(l.get(0)).trim();
                else if (raw instanceof Object[] a && a.length > 0 && a[0] != null) value = String.valueOf(a[0]).trim();
                else if (raw instanceof String s && !s.isBlank()) value = s.trim();

                if (value != null && !value.isBlank()) {
                    // Don’t use UUID-ish values as usernames
                    if ("uid".equalsIgnoreCase(key) && looksLikeUuid(value)) {
                        LOGGER.debug("Ignoring uid that looks like UUID: {}", value);
                        continue;
                    }
                    chosen = value;
                    LOGGER.debug("Selected principal id '{}' from attribute '{}'", chosen, key);
                    break;
                }
            }
        }

        try {
            return super.createPrincipal(
                (chosen != null && !chosen.isBlank()) ? chosen : originalId,
                attributes
            );
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create principal", t);
        }
    }
}

