package de.triology.cas.ldap;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Class for creating and configuring the LDAP authentication handler for the CES.
 */
@Configuration("CesAuthenticationEventExecutionPlanConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Import(LdapConfiguration.class)
@AutoConfigureAfter(LdapConfiguration.class)
@Slf4j
public class CesAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {
    private final AuthenticationHandler authenticationHandler;

    @Autowired
    CesAuthenticationEventExecutionPlanConfiguration(AuthenticationHandler cesGroupAwareLdapAuthenticationHandler) {
        this.authenticationHandler = cesGroupAwareLdapAuthenticationHandler;
    }

    @Override
    public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(authenticationHandler);
        LOGGER.trace("Registered {}, registered authentication handlers: {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), plan.getAuthenticationHandlers());
    }
}
