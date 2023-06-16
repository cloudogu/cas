package de.triology.cas.oidc.beans;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.CasProtocolConstants;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.util.EncodingUtils;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.http.FoundAction;
import org.pac4j.core.exception.http.RedirectionAction;

import java.util.Optional;

/**
 * This class handles the building of the redirection url when using the OIDC protocol. We removed the evaluation
 * of the GET parameter prompt which meddles with the general SSO/SSL flow.
 */
@RequiredArgsConstructor
@Slf4j
public class CesOidcClientRedirectActionBuilder implements OAuth20CasClientRedirectActionBuilder {

    @Override
    public Optional<RedirectionAction> build(final CasClient casClient, final WebContext context) {
        var renew = casClient.getConfiguration().isRenew();
        var gateway = casClient.getConfiguration().isGateway();

        val serviceUrl = casClient.computeFinalCallbackUrl(context);
        val casServerLoginUrl = casClient.getConfiguration().getLoginUrl();
        val redirectionUrl = casServerLoginUrl + (casServerLoginUrl.contains("?") ? "&" : "?")
                + CasProtocolConstants.PARAMETER_SERVICE + '=' + EncodingUtils.urlEncode(serviceUrl)
                + (renew ? '&' + CasProtocolConstants.PARAMETER_RENEW + "=true" : StringUtils.EMPTY)
                + (gateway ? '&' + CasProtocolConstants.PARAMETER_GATEWAY + "=true" : StringUtils.EMPTY);
        LOGGER.debug("Final redirect url is [{}]", redirectionUrl);
        Optional<RedirectionAction> action = Optional.of(new FoundAction(redirectionUrl));

        LOGGER.debug("Final redirect action is [{}]", action);
        return action;
    }
}
