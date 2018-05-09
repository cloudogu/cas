package de.triology.cas.limiting;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.authentication.AuthenticationManager;
import org.jasig.cas.authentication.Credential;

import java.util.Arrays;

/**
 * Limits authentication requests for a specific account to a limited number of attempts in a given time. If this
 * number has been exceeded, a {@link AccountTemporarilyLockedException} will be thrown until another given time has
 * passed.
 */
public class LimitingAuthenticationManager implements AuthenticationManager {

    private final AuthenticationManager delegate;
    private final TimedLoginLimiter limiter;

    public LimitingAuthenticationManager(AuthenticationManager delegate, TimedLoginLimiter limiter) {
        this.delegate = delegate;
        this.limiter = limiter;
    }

    @Override
    public Authentication authenticate(Credential... credentials) throws AuthenticationException {
        limiter.assertNotLocked(extractIds(credentials));
        try {
            return delegate.authenticate(credentials);
        } catch (AuthenticationException e) {
            Arrays.stream(credentials)
                    .map(Credential::getId)
                    .forEach(limiter::loginFailed);
            throw e;
        }
    }

    private String[] extractIds(Credential[] credentials) {
        return Arrays.stream(credentials).map(Credential::getId).toArray(String[]::new);
    }
}
