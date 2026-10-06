package de.triology.cas.pat.authentication;

import java.util.List;
import org.apereo.cas.authentication.AuthenticationHandler;
import javax.security.auth.login.LoginException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/** Authenticates API requests exclusively through the PAT handler, without password fallback. */
@lombok.extern.slf4j.Slf4j
public class PATApiAuthenticationManager implements AuthenticationManager {
    private final AuthenticationHandler handler;

    public PATApiAuthenticationManager(AuthenticationHandler handler) {
        this.handler = handler;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)
                || !(authentication.getCredentials() instanceof String token)) {
            LOGGER.warn("event=pat_authentication result=unauthorized reason=invalid_or_unsupported_credentials username={}", authentication.getName());
            throw new BadCredentialsException("Invalid PAT credentials");
        }
        var credential = new PATCredential(authentication.getName(), token);
        if (!handler.supports(credential)) {
            LOGGER.warn("event=pat_authentication result=unauthorized reason=invalid_or_unsupported_credentials username={}", authentication.getName());
            throw new BadCredentialsException("Invalid PAT credentials");
        }
        try {
            var principal = handler.authenticate(credential, null).getPrincipal();
            return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        } catch (LoginException e) {
            LOGGER.warn("event=pat_authentication result=unauthorized reason=login_failed username={} exceptionType={}", authentication.getName(), e.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid PAT credentials", e);
        } catch (Throwable e) {
            if (e instanceof Error error) {
                throw error;
            }
            LOGGER.error("event=pat_authentication result=unavailable username={} exceptionType={}", authentication.getName(), e.getClass().getSimpleName());
            throw new AuthenticationServiceException("PAT authentication unavailable", e);
        }
    }
}
