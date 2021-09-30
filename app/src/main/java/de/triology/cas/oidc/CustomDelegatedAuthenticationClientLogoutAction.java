package de.triology.cas.oidc;

import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.web.support.WebUtils;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.JEEHttpActionAdapter;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Optional;

/**
 * This is {@link CustomDelegatedAuthenticationClientLogoutAction}.
 * <p>
 * The action takes into account the currently used PAC4J client which is stored
 * in the user profile. If the client is found, its logout action is executed and
 * the user will be redirected to the specified redirect uri.
 * <p>
 * Assumption: The PAC4J user profile should be in the user session during
 * logout, accessible with PAC4J Profile Manager. The Logout web flow should
 * make sure the user profile is present.
 *
 * @author Misagh Moayyed und Christian Beyer
 * @since 6.3.0
 */
@RequiredArgsConstructor
public class CustomDelegatedAuthenticationClientLogoutAction extends AbstractAction {
    protected static final Logger LOGGER = LoggerFactory.getLogger(CustomDelegatedAuthenticationClientLogoutAction.class);
    private final Clients clients;
    private final SessionStore<JEEContext> sessionStore;
    private final String redirectUri;

    /**
     * Finds the current profile from the context.
     *
     * @param webContext A web context (request + response).
     * @return The common profile active.
     */
    private static CommonProfile findCurrentProfile(final JEEContext webContext) {
        val pm = new ProfileManager<CommonProfile>(webContext, webContext.getSessionStore());
        val profile = pm.get(true);
        return profile.orElse(null);
    }

    @Override
    protected Event doExecute(final RequestContext requestContext) {
        try {
            val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
            val response = WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext);
            val context = new JEEContext(request, response, this.sessionStore);

            val currentProfile = findCurrentProfile(context);
            val clientResult = currentProfile == null
                    ? Optional.<Client>empty()
                    : clients.findClient(currentProfile.getClientName());
            if (clientResult.isPresent()) {
                val client = clientResult.get();
                LOGGER.debug("Located client [{}] with redirect-uri [{}]", client, redirectUri);
                val actionResult = client.getLogoutAction(context, currentProfile, redirectUri);
                if (actionResult.isPresent()) {
                    val action = (HttpAction) actionResult.get();
                    new JEEHttpActionAdapter().adapt(action, context);
                }
            } else {
                LOGGER.debug("The current client cannot be found and no logout action will be executed.");
            }
        } catch (final Exception e) {
            LoggingUtils.warn(LOGGER, e);
        }
        return null;
    }
}
