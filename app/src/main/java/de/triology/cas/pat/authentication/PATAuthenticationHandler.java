package de.triology.cas.pat.authentication;

import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.config.PATRequestPathFilter;
import de.triology.cas.pat.service.PATService;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.springframework.beans.factory.ObjectProvider;

import jakarta.servlet.http.HttpServletRequest;

import javax.security.auth.login.FailedLoginException;

public class PATAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {
    private final PATService patService;
    private final CesGroupAwareLdapAuthenticationHandler ldapHandler;
    private final ObjectProvider<HttpServletRequest> requests;


    public PATAuthenticationHandler(String name, PrincipalFactory principalFactory, Integer order,
                                   PATService patService,
                                   CesGroupAwareLdapAuthenticationHandler ldapHandler,
                                   ObjectProvider<HttpServletRequest> requests) {
        super(name, principalFactory, order);
        this.patService = patService;
        this.ldapHandler = ldapHandler;
        this.requests = requests;
    }

    @Override
    public boolean supports(Credential credential) {
        return credential instanceof UsernamePasswordCredential userPassword
                && userPassword.toPassword() != null
                && userPassword.toPassword().startsWith("pat_");
    }

    @Override
    protected AuthenticationHandlerExecutionResult authenticateUsernamePasswordInternal(UsernamePasswordCredential credential, String originalPassword) throws Throwable {
        HttpServletRequest request = requests.getIfAvailable();
        String path = request == null
                ? null
                : (String) request.getAttribute(PATRequestPathFilter.REQUEST_PATH_ATTRIBUTE);
        if (path == null) {
            throw new FailedLoginException("Request path not available");
        }
        PATMetadata metadata = patService.resolve(originalPassword, path)
                .orElseThrow(() -> new FailedLoginException("Invalid or expired PAT"));

        var principal = ldapHandler.resolvePrincipal(metadata.userId());

        return createHandlerResult(credential, principal);
    }
}
