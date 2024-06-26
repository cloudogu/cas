package de.triology.cas.oidc.beans;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.support.oauth.util.OAuth20Utils;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.ticket.accesstoken.OAuth20AccessToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Currently we need the custom renderer to transform the LDAP-Groups
 * into a simplified form consisting only of the groups name.
 */
@Slf4j
public class CesOAuthProfileRenderer implements OAuth20UserProfileViewRenderer {

    /**
     * The group attributes that contains all groups of a user.
     */
    String modelAttributesGroup = "groups";

    @Override
    public ResponseEntity render(Map<String, Object> model, OAuth20AccessToken accessToken, HttpServletResponse response) {
        LOGGER.debug("Using custom profile renderer {}", model);
        val userProfile = getRenderedUserProfile(model, accessToken, response);
        return renderProfileForModel(userProfile, accessToken, response);
    }

    /**
     * Render profile for model.
     *
     * @param userProfile the user profile
     * @param accessToken the access token
     * @param response    the response
     * @return the string
     */
    protected ResponseEntity renderProfileForModel(final Map<String, Object> userProfile,
                                                   final OAuth20AccessToken accessToken, final HttpServletResponse response) {
        val json = OAuth20Utils.toJson(userProfile);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    /**
     * Gets rendered user profile.
     *
     * @param model       the model
     * @param accessToken the access token
     * @param response    the response
     * @return the rendered user profile
     */
    protected Map<String, Object> getRenderedUserProfile(final Map<String, Object> model,
                                                         final OAuth20AccessToken accessToken,
                                                         final HttpServletResponse response) {
        LOGGER.debug("Before - Profile: {}", model);
        val customModel = new LinkedHashMap<String, Object>();

        // Add all entries other than the attributes
        model.keySet()
                .stream()
                .filter(k -> !k.equalsIgnoreCase(MODEL_ATTRIBUTE_ATTRIBUTES))
                .forEach(k -> customModel.put(k, model.get(k)));

        if (model.containsKey(MODEL_ATTRIBUTE_ATTRIBUTES)) {
            Map<String, Object> attributes = (Map<String, Object>) model.get(MODEL_ATTRIBUTE_ATTRIBUTES);

            LinkedList<String> newGroups = new LinkedList<>();
            if (attributes.containsKey(modelAttributesGroup)) {
                Object groupsObject = attributes.get(modelAttributesGroup);
                LOGGER.debug("Class of object: {}", groupsObject);
                List<String> groups = (List<String>) groupsObject;
                groups.forEach(o -> newGroups.add(o));
            }
            attributes.put(modelAttributesGroup, newGroups);
            customModel.put(MODEL_ATTRIBUTE_ATTRIBUTES, attributes);
        }
        LOGGER.debug("After - Profile: {}", customModel);
        return customModel;
    }
}
