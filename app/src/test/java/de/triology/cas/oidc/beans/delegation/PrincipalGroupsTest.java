package de.triology.cas.oidc.beans.delegation;

import org.apereo.cas.authentication.principal.Principal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrincipalGroupsTest {

    @Test
    void getGroupsFromPrincipal_returnsExistingGroups() {
        Principal principal = mock(Principal.class);
        Map<String, List<Object>> attributes = new HashMap<>();
        attributes.put("groups", List.of("group1", "group2"));
        when(principal.getAttributes()).thenReturn(attributes);

        List<Object> groups = PrincipalGroups.getGroupsFromPrincipal(principal);

        assertEquals(List.of("group1", "group2"), groups);
    }

    @Test
    void getGroupsFromPrincipal_returnsEmptyListWhenMissing() {
        Principal principal = mock(Principal.class);
        when(principal.getAttributes()).thenReturn(new HashMap<>());

        List<Object> groups = PrincipalGroups.getGroupsFromPrincipal(principal);

        assertTrue(groups.isEmpty());
    }

    @Test
    void setGroupsInPrincipal_storesGroupsUnderGroupsKey() {
        Principal principal = mock(Principal.class);
        Map<String, List<Object>> attributes = new HashMap<>();
        when(principal.getAttributes()).thenReturn(attributes);

        PrincipalGroups.setGroupsInPrincipal(principal, List.of("group3"));

        assertEquals(List.of("group3"), attributes.get("groups"));
    }
}
