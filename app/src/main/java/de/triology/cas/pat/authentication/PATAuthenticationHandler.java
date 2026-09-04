package de.triology.cas.pat.authentication;

import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.service.PATService;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import javax.security.auth.login.FailedLoginException;

/**
 * Authenticates personal access tokens and carries their scopes into CAS ticket state.
 */
public class PATAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {
    private final PATService patService;
    private final CesGroupAwareLdapAuthenticationHandler ldapHandler;

    public PATAuthenticationHandler(String name, PrincipalFactory principalFactory, Integer order,
                                   PATService patService,
                                   CesGroupAwareLdapAuthenticationHandler ldapHandler) {
        super(name, principalFactory, order);
        this.patService = patService;
        this.ldapHandler = ldapHandler;
    }

    /**
     * Determines whether the credential contains a personal access token.
     *
     * @param credential credential presented for authentication
     * @return whether this handler supports the credential
     */

    @Override
    public boolean supports(Credential credential) {
        return credential instanceof UsernamePasswordCredential userPassword
                && userPassword.toPassword() != null
                && userPassword.toPassword().startsWith("pat_");
    }

    /**
     * Validates the PAT, resolves its LDAP principal, and stores its scope on the principal.
     *
     * @param credential transformed username/password credential
     * @param originalPassword original cleartext PAT
     * @return successful authentication result
     * @throws Throwable if PAT or principal resolution fails
     */

    @Override
    protected AuthenticationHandlerExecutionResult authenticateUsernamePasswordInternal(UsernamePasswordCredential credential, String originalPassword) throws Throwable {
        PATMetadata metadata = patService.resolve(originalPassword)
                .orElseThrow(() -> new FailedLoginException("Invalid or expired PAT"));

        var principal = ldapHandler.resolvePrincipal(metadata.userId());
        var attributes = new java.util.LinkedHashMap<>(principal.getAttributes());
        attributes.put(PATService.PAT_SCOPE_ATTRIBUTE, java.util.List.of(metadata.scope()));
        principal = principalFactory.createPrincipal(principal.getId(), attributes);

        return createHandlerResult(credential, principal);
    }
}
