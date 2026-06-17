package de.triology.cas.gauth;

import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.gauth.credential.GoogleAuthenticatorTokenCredential;
import org.apereo.cas.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialValidator;
import org.apereo.cas.web.flow.actions.BaseCasWebflowAction;
import org.apereo.cas.web.support.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Deletes a registered Google Authenticator device only after a second factor was validated.
 *
 * <p>CAS 7.3 changed the stock {@code GoogleAuthenticatorDeleteAccountAction} into a two-step flow:
 * the first delete event validates the OTP/scratch code and marks the account as removal-verified, and a
 * second delete event removes it. That is hard to express safely from our custom template because wrong
 * codes are thrown into CAS's generic error handling and successful removal depends on an auto-submitted
 * second form.</p>
 *
 * <p>This action keeps CAS 7.3's security property (no password-only device removal) while making the
 * webflow single-step again from the user's perspective: validate the submitted second factor, delete the
 * selected account immediately after validation, and return {@code error} instead of throwing for bad or
 * incomplete delete requests.</p>
 */
public class CesGoogleAuthenticatorDeleteAccountAction extends BaseCasWebflowAction {
    /**
     * Flash-scope marker consumed by the confirm-registration view to show the localized delete error.
     */
    public static final String ATTRIBUTE_DELETE_DEVICE_ERROR = "gauthDeleteDeviceError";

    /**
     * Message key intentionally kept separate from CAS's login token error text: this page is about deleting
     * a registered device, not authenticating into CAS.
     */
    public static final String MESSAGE_CODE_DELETE_DEVICE_ERROR = "screen.authentication.gauth.delete.invalidtoken";

    private static final String DEFAULT_DELETE_DEVICE_ERROR = "Invalid authenticator code";

    private final OneTimeTokenCredentialRepository repository;

    private final OneTimeTokenCredentialValidator<GoogleAuthenticatorTokenCredential, GoogleAuthenticatorToken> validator;

    public CesGoogleAuthenticatorDeleteAccountAction(
        final OneTimeTokenCredentialRepository repository,
        final OneTimeTokenCredentialValidator<GoogleAuthenticatorTokenCredential, GoogleAuthenticatorToken> validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    protected Event doExecuteInternal(final RequestContext requestContext) {
        requestContext.getFlashScope().remove(ATTRIBUTE_DELETE_DEVICE_ERROR);

        var requestParameters = requestContext.getRequestParameters();
        /*
         * Old CAS 7.2-style delete posts only submit accountId. Treat them as invalid instead of honoring
         * CAS 7.2's password-only device removal behavior.
         */
        if (!Boolean.parseBoolean(requestParameters.get("validate", Boolean.FALSE.toString()))) {
            return invalidCode(requestContext);
        }

        var accountId = parseAccountId(requestParameters.get("accountId"));
        var token = requestParameters.get("token");
        /*
         * Device deletion must always identify the account and prove possession of a current OTP or scratch
         * code. Missing or malformed input stays in the webflow's normal error path.
         */
        if (accountId == null || StringUtils.isBlank(token)) {
            return invalidCode(requestContext);
        }

        var account = repository.get(accountId);
        if (account == null || !isTokenValid(requestContext, account, token)) {
            return invalidCode(requestContext);
        }

        repository.delete(account.getId());
        return success();
    }

    private boolean isTokenValid(final RequestContext requestContext, final OneTimeTokenAccount account, final String token) {
        try {
            var authentication = WebUtils.getAuthentication(requestContext);
            if (authentication == null || authentication.getPrincipal() == null) {
                return false;
            }
            var tokenCredential = new GoogleAuthenticatorTokenCredential(token, account.getId());
            return validator.validate(authentication, tokenCredential) != null;
        } catch (final Throwable e) {
            /*
             * The CAS validator may return null or throw for an invalid OTP/scratch code. In both cases, the
             * user should see the confirm-registration view again rather than the generic CAS error page.
             */
            return false;
        }
    }

    private Event invalidCode(final RequestContext requestContext) {
        requestContext.getFlashScope().put(ATTRIBUTE_DELETE_DEVICE_ERROR, Boolean.TRUE);
        WebUtils.addErrorMessageToContext(requestContext, MESSAGE_CODE_DELETE_DEVICE_ERROR, DEFAULT_DELETE_DEVICE_ERROR);
        return error();
    }

    private static Long parseAccountId(final String accountId) {
        if (StringUtils.isBlank(accountId)) {
            return null;
        }
        try {
            return Long.parseLong(accountId);
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
