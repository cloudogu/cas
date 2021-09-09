package de.triology.cas.services.attributes;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnMappedAttributesPolicyTest extends TestCase {

    public void testAuthorizeReleaseOfAllowedAttributes_withMapping() {
        // given
        List<String> allowedAttributes = List.of("username", "mail");
        Map<String, String> attributesMappingRules = Map.of("preferred_username", "username");
        ReturnMappedAttributesPolicy policy = new ReturnMappedAttributesPolicy(allowedAttributes, attributesMappingRules);

        Map<String, List<Object>> inputAttributes = new HashMap<>();
        inputAttributes.put("preferred_username", List.of("testUsername"));
        inputAttributes.put("mail", List.of("super@duper.de"));
        inputAttributes.put("ignoreMe", List.of("ignoring"));

        // when
        Map<String, List<Object>> returnedAttributes = policy.authorizeReleaseOfAllowedAttributes(null, inputAttributes, null, null);

        // then
        assertEquals(returnedAttributes.size(), 2);
        assertEquals(returnedAttributes.get("mail"), List.of("super@duper.de"));
        assertEquals(returnedAttributes.get("username"), List.of("testUsername"));
        assertFalse(returnedAttributes.containsKey("ignoreMe"));
    }

    public void testAuthorizeReleaseOfAllowedAttributes_withoutMapping() {
        // given
        List<String> allowedAttributes = List.of("username", "mail");
        ReturnMappedAttributesPolicy policy = new ReturnMappedAttributesPolicy(allowedAttributes, null);

        Map<String, List<Object>> inputAttributes = new HashMap<>();
        inputAttributes.put("preferred_username", List.of("testUsername"));
        inputAttributes.put("mail", List.of("super@duper.de"));
        inputAttributes.put("ignoreMe", List.of("ignoring"));

        // when
        Map<String, List<Object>> returnedAttributes = policy.authorizeReleaseOfAllowedAttributes(null, inputAttributes, null, null);

        // then
        assertEquals(returnedAttributes.size(), 1);
        assertEquals(returnedAttributes.get("mail"), List.of("super@duper.de"));
        assertFalse(returnedAttributes.containsKey("username"));
        assertFalse(returnedAttributes.containsKey("ignoreMe"));
    }

    public void testAuthorizeReleaseOfAllowedAttributes_noAllowedAttributes() {
        // given
        ReturnMappedAttributesPolicy policy = new ReturnMappedAttributesPolicy(null, null);

        Map<String, List<Object>> inputAttributes = new HashMap<>();
        inputAttributes.put("preferred_username", List.of("testUsername"));
        inputAttributes.put("mail", List.of("super@duper.de"));
        inputAttributes.put("ignoreMe", List.of("ignoring"));

        // when
        Map<String, List<Object>> returnedAttributes = policy.authorizeReleaseOfAllowedAttributes(null, inputAttributes, null, null);

        // then
        assertEquals(returnedAttributes.size(), 0);
        assertFalse(returnedAttributes.containsKey("mail"));
        assertFalse(returnedAttributes.containsKey("username"));
        assertFalse(returnedAttributes.containsKey("ignoreMe"));
    }
}