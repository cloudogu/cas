package de.triology.cas.oidc.beans.delegation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
@Getter
public class AttributeMapping {
    private final String source;
    private final String target;

    /**
     * Generates a List of AttributeMappings from a given property (string) of the following format:
     * this.is.my.property.key=value1:key1,value2:key2
     *
     * @param propertyString The content of the properties value
     * @return List<AttributeMapping>
     */
    public static List<AttributeMapping> fromPropertyString(String propertyString) {
        List<AttributeMapping> mappings = new ArrayList<>();
        if (propertyString.isBlank()) {
            return mappings;
        }

        String[] mappingStrings = propertyString.split(",");
        Arrays.stream(mappingStrings).forEach(ms -> {
            String[] mapping = ms.split(":");
            mappings.add(new AttributeMapping(mapping[0], mapping[1]));
        });
        return mappings;
    }
}
