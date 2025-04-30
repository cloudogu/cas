package de.triology.cas.oidc.beans;

import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesOAuthProfileRenderer}.
 */
class CesOAuthProfileRendererTests {

    private CesOAuthProfileRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new CesOAuthProfileRenderer();
    }

    @Test
    void render_ShouldReturnJsonResponse_WhenGroupsArePresent() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("groups", List.of("admin", "user"));
        
        Map<String, Object> model = new HashMap<>();
        model.put(CesOAuthProfileRenderer.MODEL_ATTRIBUTE_ATTRIBUTES, attributes);

        OAuth20AccessToken accessToken = mock(OAuth20AccessToken.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // when
        ResponseEntity<String> entity = renderer.render(model, accessToken, response);

        // then
        assertNotNull(entity, "ResponseEntity should not be null");
        assertEquals(200, entity.getStatusCodeValue(), "ResponseEntity should return 200 OK");
        assertTrue(entity.getBody().contains("admin"), "Response JSON should contain 'admin'");
        assertTrue(entity.getBody().contains("user"), "Response JSON should contain 'user'");
    }

    @Test
    void render_ShouldAddEmptyGroups_WhenGroupsAreMissing() {
        // given
        Map<String, Object> attributes = new HashMap<>(); // no groups
        Map<String, Object> model = new HashMap<>();
        model.put(CesOAuthProfileRenderer.MODEL_ATTRIBUTE_ATTRIBUTES, attributes);

        OAuth20AccessToken accessToken = mock(OAuth20AccessToken.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // when
        ResponseEntity<String> entity = renderer.render(model, accessToken, response);

        // then
        assertNotNull(entity, "ResponseEntity should not be null");
        assertEquals(200, entity.getStatusCodeValue(), "ResponseEntity should return 200 OK");
        assertTrue(entity.getBody().contains("\"groups\":[]"), "Response JSON should contain empty groups array");
    }

    @Test
    void render_ShouldHandleEmptyModel() {
        // given
        Map<String, Object> model = new HashMap<>();
        OAuth20AccessToken accessToken = mock(OAuth20AccessToken.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // when
        ResponseEntity<String> entity = renderer.render(model, accessToken, response);

        // then
        assertNotNull(entity, "ResponseEntity should not be null");
        assertEquals(200, entity.getStatusCodeValue(), "Should still return 200 OK for empty model");
    }

    @Test
    void getRenderedUserProfile_ShouldCopyModelAndAddGroups_WhenMissing() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> model = new HashMap<>();
        model.put(CesOAuthProfileRenderer.MODEL_ATTRIBUTE_ATTRIBUTES, attributes);

        // when
        Map<String, Object> result = renderer.getRenderedUserProfile(model);

        // then
        assertTrue(result.containsKey("groups"), "Groups key should be added when missing");
        assertTrue(result.get("groups") instanceof List, "Groups should be a list");
    }

    @Test
    void getRenderedUserProfile_ShouldKeepExistingGroups() {
        // given
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("groups", List.of("admin"));
        Map<String, Object> model = new HashMap<>();
        model.put(CesOAuthProfileRenderer.MODEL_ATTRIBUTE_ATTRIBUTES, attributes);

        // when
        Map<String, Object> result = renderer.getRenderedUserProfile(model);

        // then
        assertEquals(List.of("admin"), result.get("groups"), "Existing groups should be preserved");
    }
}
