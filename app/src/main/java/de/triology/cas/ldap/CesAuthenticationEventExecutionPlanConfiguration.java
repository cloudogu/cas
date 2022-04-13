package de.triology.cas.ldap;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CesAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {
    private static final Logger LOG = LoggerFactory.getLogger(CesAuthenticationEventExecutionPlanConfiguration.class);

    private final AuthenticationHandler authenticationHandler;

    @Autowired
    CesAuthenticationEventExecutionPlanConfiguration(AuthenticationHandler cesGroupAwareLdapAuthenticationHandler) {
        this.authenticationHandler = cesGroupAwareLdapAuthenticationHandler;
    }

    @Override
    public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(authenticationHandler);
        LOG.trace("Registered {}, registered authentication handlers: {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), plan.getAuthenticationHandlers());
    }
}
