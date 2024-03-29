package de.triology.cas.services.attributes;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.services.AbstractRegisteredServiceAttributeReleasePolicy;
import org.apereo.cas.services.RegisteredServiceAttributeReleasePolicyContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Return all attributes required for the ecosystem
 */
@ToString(callSuper = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@Slf4j
public class ReturnMappedAttributesPolicy extends AbstractRegisteredServiceAttributeReleasePolicy {

    private List<String> allowedAttributes;
    private Map<String, String> attributesMappingRules;

    @Override
    public Map<String, List<Object>> getAttributesInternal(RegisteredServiceAttributeReleasePolicyContext context,
                                                           Map<String, List<Object>> attributes) {
        return authorizeReleaseOfAllowedAttributes(context, attributes);
    }

    /**
     * Authorize release of allowed attributes map.
     *
     * @param attrs the attributes
     * @return the map
     */
    protected Map<String, List<Object>> authorizeReleaseOfAllowedAttributes(RegisteredServiceAttributeReleasePolicyContext context,
                                                                            Map<String, List<Object>> attrs) {
        HashMap<String, List<Object>> attributesToRelease = new HashMap<>();
        if (allowedAttributes == null) {
            return attributesToRelease;
        }

        // map attributes
        Map<String, List<Object>> mappedAttributes = mapAttributes(attrs);

        // order attributes
        TreeMap<String, List<Object>> resolvedAttributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        resolvedAttributes.putAll(mappedAttributes);

        // filter attributes
        getAllowedAttributes()
                .stream()
                .filter(resolvedAttributes::containsKey)
                .forEach(attr -> {
                    LOGGER.debug("Found attribute [{}] in the list of allowed attributes", attr);
                    attributesToRelease.put(attr, resolvedAttributes.get(attr));
                });


        LOGGER.debug("Attributes to release [{}]", resolvedAttributes);
        return attributesToRelease;
    }

    @Override
    protected List<String> determineRequestedAttributeDefinitions(final RegisteredServiceAttributeReleasePolicyContext context) {
        return getAllowedAttributes();
    }

    protected Map<String, List<Object>> mapAttributes(Map<String, List<Object>> attributes) {
        if (attributesMappingRules == null) {
            return attributes;
        }

        LOGGER.debug("Start mapping of attributes with the following rules [{}]", attributesMappingRules);

        Map<String, List<Object>> mappedAttributes = new TreeMap<>();
        attributes.keySet().stream().filter(attributesMappingRules::containsKey).forEach(s -> {
            LOGGER.debug("Transform attribute [{}] -> [{}]", s, attributesMappingRules.get(s));
            mappedAttributes.put(attributesMappingRules.get(s), attributes.get(s));
        });
        attributes.keySet().stream().filter(s -> !attributesMappingRules.containsKey(s)).forEach(s -> mappedAttributes.put(s, attributes.get(s)));
        return mappedAttributes;
    }
}
