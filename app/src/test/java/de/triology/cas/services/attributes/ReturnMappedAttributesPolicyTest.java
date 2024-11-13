package de.triology.cas.services.attributes;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReturnMappedAttributesPolicyTest extends TestCase {
    public void testAuthorizeReleaseOfAllowedAttribute() {
        // given
        List<String> allowedAttributes = List.of("username", "mail");
        ReturnMappedAttributesPolicy policy = new ReturnMappedAttributesPolicy(allowedAttributes);

        Map<String, List<Object>> inputAttributes = new HashMap<>();
        inputAttributes.put("username", List.of("testUsername"));
        inputAttributes.put("mail", List.of("super@duper.de"));
        inputAttributes.put("ignoreMe", List.of("ignoring"));

        // when
        Map<String, List<Object>> returnedAttributes = policy.authorizeReleaseOfAllowedAttributes(null, inputAttributes);

        // then
        assertEquals(returnedAttributes.size(), 2);
        assertEquals(returnedAttributes.get("username"), List.of("testUsername"));
        assertEquals(returnedAttributes.get("mail"), List.of("super@duper.de"));
        assertFalse(returnedAttributes.containsKey("ignoreMe"));
    }

    public void testAuthorizeReleaseOfAllowedAttributes_noAllowedAttributes() {
        // given
        ReturnMappedAttributesPolicy policy = new ReturnMappedAttributesPolicy(null);

        Map<String, List<Object>> inputAttributes = new HashMap<>();
        inputAttributes.put("preferred_username", List.of("testUsername"));
        inputAttributes.put("mail", List.of("super@duper.de"));
        inputAttributes.put("ignoreMe", List.of("ignoring"));

        // when
        Map<String, List<Object>> returnedAttributes = policy.authorizeReleaseOfAllowedAttributes(null, inputAttributes);

        // then
        assertEquals(returnedAttributes.size(), 0);
        assertFalse(returnedAttributes.containsKey("mail"));
        assertFalse(returnedAttributes.containsKey("username"));
        assertFalse(returnedAttributes.containsKey("ignoreMe"));
    }
}