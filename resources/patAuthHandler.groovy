import org.apereo.cas.authentication.*
import org.apereo.cas.authentication.credential.*
import org.apereo.cas.authentication.metadata.*

import javax.security.auth.login.*

def authenticate(final Object... args) {
    def (authenticationHandler,credential,servicesManager,principalFactory,logger) = args

    logger.error("PAT Authentication has been called!!!")

    def password = (UsernamePasswordCredential) credential;

    if (password.toPassword().equals("testPassword")) {
        def principal = principalFactory.createPrincipal(credential.username);
        return new DefaultAuthenticationHandlerExecutionResult(authenticationHandler,
                credential, principal, new ArrayList<>(0));
    }

    throw new FailedLoginException();
}

def supportsCredential(final Object... args) {
    def (credential,logger) = args
    logger.error("PAT Authentication supportsCredential has been called!!!")

    return true
}

def supportsCredentialClass(final Object... args) {
    def (credentialClazz,logger) = args

    logger.error("PAT Authentication supportsCredentialClass has been called!!!")
    return true
}