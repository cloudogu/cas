package de.triology.cas.oidc.beans.delegation;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.web.support.WebUtils;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.http.adapter.JEEHttpActionAdapter;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.jee.context.JEEContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Optional;

/**
 * This is {@link CesCustomDelegatedAuthenticationClientLogoutAction}.
 * <p>
 * The action takes into account the currently used PAC4J client which is stored
 * in the user profile. If the client is found, its logout action is executed and
 * the user will be redirected to the specified redirect uri.
 * <p>
 * Assumption: The PAC4J user profile should be in the user session during
 * logout, accessible with PAC4J Profile Manager. The Logout web flow should
 * make sure the user profile is present.
 */
@RequiredArgsConstructor
public class CesCustomDelegatedAuthenticationClientLogoutAction extends AbstractAction {
    protected static final Logger LOG = LoggerFactory.getLogger(CesCustomDelegatedAuthenticationClientLogoutAction.class);
    private final Clients clients;
    private final SessionStore sessionStore;
    private final String redirectUri;

    /**
     * Finds the current profile from the context.
     *
     * @param webContext A web context (request + response).
     * @return The common profile active.
     */
    private UserProfile findCurrentProfile(final JEEContext webContext) {
        val pm = new ProfileManager(webContext, this.sessionStore);
        val profile = pm.getProfile();
        return profile.orElse(null);
    }

    @Override
    protected Event doExecute(final RequestContext requestContext) {
        try {
            val request = WebUtils.getHttpServletRequestFromExternalWebflowContext(requestContext);
            val response = WebUtils.getHttpServletResponseFromExternalWebflowContext(requestContext);
            val context = new JEEContext(request, response);

            val currentProfile = findCurrentProfile(context);
            val clientResult = currentProfile == null
                    ? Optional.<Client>empty()
                    : clients.findClient(currentProfile.getClientName());
            if (clientResult.isPresent()) {
                val client = clientResult.get();
                LOG.debug("Located client [{}] with redirect-uri [{}]", client, redirectUri);
                val actionResult = client.getLogoutAction(new CallContext(context, this.sessionStore), currentProfile, redirectUri);
                if (actionResult.isPresent()) {
                    val action = (HttpAction) actionResult.get();
                    LOG.debug("Adapting logout action [{}] for client [{}]", action, client);
                    JEEHttpActionAdapter.INSTANCE.adapt(action, context);
                }
            } else {
                LOG.debug("The current client cannot be found and no logout action will be executed.");
            }
        } catch (final Exception e) {
            LoggingUtils.warn(LOG, e);
        }
        return null;
    }
}
