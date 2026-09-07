package de.triology.cas.principal;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link AttributeSelectingPrincipalFactory}.
 */
class AttributeSelectingPrincipalFactoryTest {

    @Test
    void createPrincipal_NullAttributes_UsesOriginalId() {
        var factory = new AttributeSelectingPrincipalFactory("uid", "mail");

        Principal principal = factory.createPrincipal("original-id", null);

        assertNotNull(principal);
        assertEquals("original-id", principal.getId());
    }

    @Test
    void createPrincipal_NoCandidateMatches_UsesOriginalId() {
        var factory = new AttributeSelectingPrincipalFactory("uid", "mail");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("unrelated", List.of("value"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("original-id", principal.getId());
    }

    @Test
    void createPrincipal_ListAttributeFirstMatch_UsesTrimmedValue() {
        var factory = new AttributeSelectingPrincipalFactory("mail", "uid");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("mail", List.of(" dustin@cloudogu.com "));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("dustin@cloudogu.com", principal.getId());
        assertEquals(attributes, principal.getAttributes());
    }

    @Test
    void createPrincipal_ArrayAttribute_UsesFirstElement() {
        var factory = new AttributeSelectingPrincipalFactory("mail");
        // A raw Object[] value stored directly under the key (bypassing the List<Object> type,
        // since attributes.get(key) never legitimately returns a bare array through the typed API).
        Map<String, Object> rawAttributes = new HashMap<>();
        rawAttributes.put("mail", new Object[]{" array-value ", "second"});
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, List<Object>> attributes = (Map) rawAttributes;

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("array-value", principal.getId());
    }

    @Test
    void createPrincipal_StringAttribute_UsesTrimmedValue() {
        var factory = new AttributeSelectingPrincipalFactory("displayName");
        Map<String, List<Object>> attributes = new HashMap<>();
        // A raw (non-List) String value stored directly under the key.
        Map<String, Object> rawAttributes = new HashMap<>();
        rawAttributes.put("displayName", " Dustin Hoffman ");
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, List<Object>> castAttributes = (Map) rawAttributes;

        Principal principal = factory.createPrincipal("original-id", castAttributes);

        assertEquals("Dustin Hoffman", principal.getId());
    }

    @Test
    void createPrincipal_EmptyListAttribute_SkipsToNextCandidate() {
        var factory = new AttributeSelectingPrincipalFactory("mail", "uid");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("mail", List.of());
        attributes.put("uid", List.of("fallback-uid"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("fallback-uid", principal.getId());
    }

    @Test
    void createPrincipal_ListWithNullFirstElement_SkipsToNextCandidate() {
        var factory = new AttributeSelectingPrincipalFactory("mail", "uid");
        Map<String, List<Object>> attributes = new HashMap<>();
        List<Object> withNull = new java.util.ArrayList<>();
        withNull.add(null);
        attributes.put("mail", withNull);
        attributes.put("uid", List.of("fallback-uid"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("fallback-uid", principal.getId());
    }

    @Test
    void createPrincipal_BlankStringAttribute_SkipsToNextCandidate() {
        var factory = new AttributeSelectingPrincipalFactory("mail", "uid");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("mail", List.of("   "));
        attributes.put("uid", List.of("fallback-uid"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("fallback-uid", principal.getId());
    }

    @Test
    void createPrincipal_UnrecognizedAttributeType_SkipsToNextCandidate() {
        var factory = new AttributeSelectingPrincipalFactory("mail", "uid");
        // A raw Integer value stored directly under the key: not a List, Object[], or String,
        // so none of the recognized branches match and this candidate is skipped.
        Map<String, Object> rawAttributes = new HashMap<>();
        rawAttributes.put("mail", 42);
        rawAttributes.put("uid", List.of("fallback-uid"));
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<String, List<Object>> attributes = (Map) rawAttributes;

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("fallback-uid", principal.getId());
    }

    @Test
    void createPrincipal_UidLooksLikeUuid_SkipsUidCandidate_UsesNextCandidate() {
        var factory = new AttributeSelectingPrincipalFactory("uid", "mail");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("uid", List.of("550e8400-e29b-41d4-a716-446655440000"));
        attributes.put("mail", List.of("dustin@cloudogu.com"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("dustin@cloudogu.com", principal.getId());
    }

    @Test
    void createPrincipal_UidCaseInsensitiveMatch_LooksLikeUuid_IsSkipped() {
        var factory = new AttributeSelectingPrincipalFactory("UID", "mail");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("UID", List.of("550E8400-E29B-41D4-A716-446655440000"));
        attributes.put("mail", List.of("dustin@cloudogu.com"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("dustin@cloudogu.com", principal.getId());
    }

    @Test
    void createPrincipal_UidNotUuid_UsesUidValue() {
        var factory = new AttributeSelectingPrincipalFactory("uid", "mail");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("uid", List.of("dhoffman"));
        attributes.put("mail", List.of("dustin@cloudogu.com"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("dhoffman", principal.getId());
    }

    @Test
    void createPrincipal_FirstCandidateMissing_FallsThroughToSecond() {
        var factory = new AttributeSelectingPrincipalFactory("username", "mail");
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("mail", List.of("dustin@cloudogu.com"));

        Principal principal = factory.createPrincipal("original-id", attributes);

        assertEquals("dustin@cloudogu.com", principal.getId());
    }
}
