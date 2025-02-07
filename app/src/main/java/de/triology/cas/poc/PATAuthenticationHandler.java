package de.triology.cas.poc;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.*;
import org.apereo.cas.authentication.credential.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.services.ServicesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.FailedLoginException;

@Slf4j
@Service
public class PATAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {

    @Autowired
    protected PATAuthenticationHandler(String name, ServicesManager servicesManager, PrincipalFactory principalFactory, Integer order) {
        super(name, servicesManager, principalFactory, order);
    }

    @Override
    protected AuthenticationHandlerExecutionResult authenticateUsernamePasswordInternal(
            final UsernamePasswordCredential credential,
            final String originalPassword) throws Throwable {
        LOGGER.info("The Handler for PAT has been called");
        try {
            if (everythingLooksGood()) {
                return createHandlerResult(credential,
                        principalFactory.createPrincipal(credential.getUsername()), null);
            }
            throw new FailedLoginException("Sorry, you are a failure!");
        } catch (FailedLoginException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean everythingLooksGood() {
        return true;
    }

}