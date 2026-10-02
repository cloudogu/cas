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
public class PATApiAuthenticationManager implements AuthenticationManager {
    private final AuthenticationHandler handler;

    public PATApiAuthenticationManager(AuthenticationHandler handler) {
        this.handler = handler;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (!(authentication instanceof UsernamePasswordAuthenticationToken)
                || !(authentication.getCredentials() instanceof String token)) {
            throw new BadCredentialsException("Invalid PAT credentials");
        }
        var credential = new PATCredential(authentication.getName(), token);
        if (!handler.supports(credential)) {
            throw new BadCredentialsException("Invalid PAT credentials");
        }
        try {
            var principal = handler.authenticate(credential, null).getPrincipal();
            return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
        } catch (LoginException e) {
            throw new BadCredentialsException("Invalid PAT credentials", e);
        } catch (Throwable e) {
            if (e instanceof Error error) {
                throw error;
            }
            throw new AuthenticationServiceException("PAT authentication unavailable", e);
        }
    }
}
