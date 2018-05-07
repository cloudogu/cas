package de.triology.cas.limiting;

import org.jasig.cas.authentication.Authentication;
import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.authentication.AuthenticationManager;
import org.jasig.cas.authentication.Credential;

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
        limiter.assertNotLocked(credentials[0].getId());
        try {
            return delegate.authenticate(credentials);
        } catch (AuthenticationException e) {
            limiter.loginFailed(credentials[0].getId());
            throw e;
        }
    }
}
