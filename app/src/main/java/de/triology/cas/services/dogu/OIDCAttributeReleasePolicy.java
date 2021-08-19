package de.triology.cas.services.dogu;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.AbstractRegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Return all attributes required for the ecosystem
 */
@ToString(callSuper = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class OIDCAttributeReleasePolicy extends AbstractRegisteredServiceAttributeReleasePolicy {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> allowedAttributes = new ArrayList<>(0);

    @Override
    public Map<String, List<Object>> getAttributesInternal(final Principal principal, final Map<String, List<Object>> attrs,
                                                           final RegisteredService registeredService, final Service selectedService) {
        return authorizeReleaseOfAllowedAttributes(principal, attrs, registeredService, selectedService);
    }

    /**
     * Authorize release of allowed attributes map.
     *
     * @param principal         the principal
     * @param attrs             the attributes
     * @param registeredService the registered service
     * @param selectedService   the selected service
     * @return the map
     */
    protected Map<String, List<Object>> authorizeReleaseOfAllowedAttributes(final Principal principal,
                                                                            final Map<String, List<Object>> attrs,
                                                                            final RegisteredService registeredService,
                                                                            final Service selectedService) {
        Map<String, List<Object>> mappedAttributes = mapAttributes(attrs);
        val resolvedAttributes = new TreeMap<String, List<Object>>(String.CASE_INSENSITIVE_ORDER);
        resolvedAttributes.putAll(mappedAttributes);

        val attributesToRelease = new HashMap<String, List<Object>>();

        attrs.forEach((s, objects) -> {
            logger.debug("Key [{}] - Value [{}]", s, objects);
        });

        getAllowedAttributes()
                .stream()
                .filter(resolvedAttributes::containsKey)
                .forEach(attr -> {
                    logger.debug("Found attribute [{}] in the list of allowed attributes", attr);
                    attributesToRelease.put(attr, resolvedAttributes.get(attr));
                });
        return attributesToRelease;
    }

    @Override
    protected List<String> getRequestedDefinitions() {
        return getAllowedAttributes();
    }

    protected Map<String, List<Object>> mapAttributes(Map<String, List<Object>> attributes) {
        // rules
        Map<String, List<Object>> mappedMap = new TreeMap<String, List<Object>>();
        attributes.forEach((s, objects) -> {
            if(s.equals("family_name")) {
                mappedMap.put("surname", objects);
            } else if (s.equals("email")) {
                mappedMap.put("mail", objects);
            } else if (s.equals("given_name")) {
                mappedMap.put("givenName", objects);
            } else if (s.equals("preferred_username")) {
                mappedMap.put("username", objects);
            } else if (s.equals("name")) {
                mappedMap.put("displayName", objects);
            } else {
                mappedMap.put(s, objects);
            }
        });
        return mappedMap;
    }
}
