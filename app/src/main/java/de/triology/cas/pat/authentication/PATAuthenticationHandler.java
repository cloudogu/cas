package de.triology.cas.pat.authentication;

import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;

import javax.security.auth.login.FailedLoginException;

public class PATAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {
    private final PATService patService;

    public PATAuthenticationHandler(String name, PrincipalFactory principalFactory, Integer order, PATService patService) {
        super(name, principalFactory, order);
        this.patService = patService;
    }

    @Override
    public boolean supports(Credential credential) {
        return credential instanceof UsernamePasswordCredential userPassword
                && userPassword.toPassword() != null
                && userPassword.toPassword().startsWith("pat_");
    }

    @Override
    protected AuthenticationHandlerExecutionResult authenticateUsernamePasswordInternal(UsernamePasswordCredential credential, String originalPassword) throws Throwable {
        // check if pat is enabled

        if (!patService.validate(credential.getUsername(), originalPassword)) {
            throw new FailedLoginException("Invalid or expired PAT");
        }

        return createHandlerResult(
                credential,
                principalFactory.createPrincipal(credential.getUsername()));
    }
}