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
    public ResponseEntity<String> render(Map<String, Object> model, OAuth20AccessToken accessToken, HttpServletResponse response) {
        LOGGER.debug("Using custom profile renderer {}", model);
        val userProfile = getRenderedUserProfile(model);
        return renderProfileForModel(userProfile);
    }

    /**
     * Render profile for model.
     *
     * @param userProfile the user profile
     * @return the string
     */
    protected ResponseEntity<String> renderProfileForModel(final Map<String, Object> userProfile) {
        val json = OAuth20Utils.toJson(userProfile);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    /**
     * Gets rendered user profile.
     *
     * @param model       the model
     * @return the rendered user profile
     */
    protected Map<String, Object> getRenderedUserProfile(final Map<String, Object> model) {
        LOGGER.debug("Before - Profile: {}", model);
        val customModel = new LinkedHashMap<>(model);

        if (model.containsKey(MODEL_ATTRIBUTE_ATTRIBUTES)) {
            val attributes = (Map<String, Object>) customModel.get(MODEL_ATTRIBUTE_ATTRIBUTES);

            if (!attributes.containsKey(modelAttributesGroup)) {
                attributes.put(modelAttributesGroup, List.of());
            }

            customModel.putAll(attributes);
        }

        LOGGER.debug("After - Profile: {}", customModel);
        return customModel;
    }
}
