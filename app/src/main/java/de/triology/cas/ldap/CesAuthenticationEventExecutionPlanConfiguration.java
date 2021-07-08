package de.triology.cas.ldap;

import lombok.val;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.support.password.DefaultPasswordPolicyHandlingStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.LdapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("CesAuthenticationEventExecutionPlanConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CesAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesAuthenticationEventExecutionPlanConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Bean
    public AuthenticationHandler cesGroupAwareLdapAuthenticationHandler() {

        val l = casProperties.getAuthn().getLdap().get(0);

        val handler = new CesGroupAwareLdapAuthenticationHandler(l.getName(),
                servicesManager.getObject(), PrincipalFactoryUtils.newPrincipalFactory(),
                l.getOrder(), LdapUtils.newLdaptiveAuthenticator(l), new DefaultPasswordPolicyHandlingStrategy());

        LOGGER.error("cesGroupAwareLdapAuthenticationHandler created");

        return handler;
    }


    @Override
    public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
        LOGGER.error("configureAuthenticationExecutionPlan");
        plan.registerAuthenticationHandler(cesGroupAwareLdapAuthenticationHandler());
    }

    @Override
    public String getName() {
        return AuthenticationEventExecutionPlanConfigurer.super.getName();
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
