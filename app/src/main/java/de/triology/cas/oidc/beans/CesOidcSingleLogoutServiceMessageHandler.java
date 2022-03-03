package de.triology.cas.oidc.beans;

import de.triology.cas.oidc.UnescapedHttpMessage;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpResponse;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.logout.LogoutHttpMessage;
import org.apereo.cas.logout.slo.SingleLogoutMessage;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutRequestContext;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.oidc.slo.OidcSingleLogoutServiceMessageHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.HttpUtils;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.util.http.HttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * The message handler for the OAuth protocol. This message handler takes action when a OAuth service should be
 * logged out of the CAS.
 */
@Slf4j
public class CesOidcSingleLogoutServiceMessageHandler extends OidcSingleLogoutServiceMessageHandler {
    protected static final Logger LOG = LoggerFactory.getLogger(CesOidcSingleLogoutServiceMessageHandler.class);

    public CesOidcSingleLogoutServiceMessageHandler(final HttpClient httpClient,
                                                    final SingleLogoutMessageCreator logoutMessageBuilder,
                                                    final ServicesManager servicesManager,
                                                    final SingleLogoutServiceLogoutUrlBuilder singleLogoutServiceLogoutUrlBuilder,
                                                    final boolean asynchronous,
                                                    final AuthenticationServiceSelectionPlan authenticationRequestServiceSelectionStrategies,
                                                    final String issuer
    ) {
        super(httpClient, logoutMessageBuilder, servicesManager, singleLogoutServiceLogoutUrlBuilder,
                asynchronous, authenticationRequestServiceSelectionStrategies, issuer);
    }

    @Override
    protected boolean sendMessageToEndpoint(final LogoutHttpMessage msg, final SingleLogoutRequestContext request, final SingleLogoutMessage logoutMessage) {
        val payload = logoutMessage.getPayload();

        HttpResponse response = null;
        try {
            HttpMessage message = new UnescapedHttpMessage(
                    new URL("http://192.168.56.1:8080"),
                    "logout_token:" + payload
            );
            message.setContentType("application/x-www-form-urlencoded");
            LOG.trace("Sending [{}] to [{}]", message, msg.getUrl().toExternalForm());
            boolean msgResponse = this.getHttpClient().sendMessageToEndPoint(message);
            LOG.trace("Message response was: [{}]", msgResponse);

            return msgResponse;
        } catch (final Exception e) {
            LoggingUtils.error(LOG, e);
        } finally {
            HttpUtils.close(response);
        }
        LOG.warn("No (successful) logout response received from the url [{}]", msg.getUrl().toExternalForm());
        return false;
    }
}
