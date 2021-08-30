package de.triology.cas.services;

import junit.framework.TestCase;

import java.util.Map;

public class CesServicesSpringConfigurationTest extends TestCase {

    public void testPropertyStringToMap_emptyString() {
        // given
        // when
        Map<String, String> propertyMap = CesServicesSpringConfiguration.propertyStringToMap("");

        // then
        assertEquals(propertyMap.size(), 0);
    }

    public void testPropertyStringToMap_withValues() {
        // given
        String mapProperty = "key1:value1,key2:value2";

        // when
        Map<String, String> propertyMap = CesServicesSpringConfiguration.propertyStringToMap(mapProperty);

        // then
        assertEquals(propertyMap.size(), 2);
        assertEquals(propertyMap.get("key1"), "value1");
        assertEquals(propertyMap.get("key2"), "value2");
    }
}