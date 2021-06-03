package de.triology.cas.oauth.web;

import de.triology.cas.oauth.CesOAuthConstants;
import de.triology.cas.oauth.CesOAuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This controller is the main entry point for OAuth version 2.0
 * wrapping in CAS, should be mapped to something like /oauth2.0/*. Dispatch
 * request to specific controllers : authorize, accessToken...
 *
 * We need a custom controller as CAS the original controller only supports plain text responds.
 * However, we require JSON support.
 */
public final class CesOAuth20WrapperController extends CesBaseOAuthWrapperController implements InitializingBean {

    private AbstractController authorizeController;

    private AbstractController callbackAuthorizeController;

    private AbstractController accessTokenController;

    private AbstractController profileController;

    private final Logger logger = LoggerFactory.getLogger(CesOAuth20CallbackAuthorizeController.class);

    @Override
    public void afterPropertiesSet() {
        authorizeController = new CesOAuth20AuthorizeController(servicesManager, loginUrl);
        callbackAuthorizeController = new CesOAuth20CallbackAuthorizeController();
        accessTokenController = new CesOAuth20AccessTokenController(servicesManager, ticketRegistry, timeout);
        profileController = new CesOAuth20ProfileController(ticketRegistry);
    }

    @Override
    protected ModelAndView internalHandleRequest(final String method, final HttpServletRequest request,
                                                 final HttpServletResponse response) throws Exception {

        // authorize
        if (CesOAuthConstants.AUTHORIZE_URL.equals(method)) {
            return authorizeController.handleRequest(request, response);
        }
        // callback on authorize
        if (CesOAuthConstants.CALLBACK_AUTHORIZE_URL.equals(method)) {
            return callbackAuthorizeController.handleRequest(request, response);
        }
        //get access token
        if (CesOAuthConstants.ACCESS_TOKEN_URL.equals(method)) {
            return accessTokenController.handleRequest(request, response);
        }
        // get profile
        if (CesOAuthConstants.PROFILE_URL.equals(method)) {
            return profileController.handleRequest(request, response);
        }

        // else error
        logger.error("Unknown method : {}", method);
        CesOAuthUtils.writeTextError(response, CesOAuthConstants.INVALID_REQUEST, 200);
        return null;
    }
}
