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

    @Override
    public Principal createPrincipal(final String originalId,
                                     final Map<String, List<Object>> attributes) {
        String chosen = null;
        if (attributes != null) {
            for (String key : idAttributeCandidates) {
                Object raw = attributes.get(key);
                if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) != null) {
                    chosen = String.valueOf(list.get(0)).trim();
                } else if (raw instanceof Object[] arr && arr.length > 0 && arr[0] != null) {
                    chosen = String.valueOf(arr[0]).trim();
                } else if (raw instanceof String s && !s.isBlank()) {
                    chosen = s.trim();
                }
                if (chosen != null && !chosen.isBlank()) {
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
        }    }
}
