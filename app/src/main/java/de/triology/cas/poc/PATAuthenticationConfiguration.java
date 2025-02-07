package de.triology.cas.poc;


import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.support.password.PasswordPolicyContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

@Configuration("PATAuthenticationEventExecutionPlanConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class PATAuthenticationConfiguration {


    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public AuthenticationHandler patAuthenticationHandler(
            @Qualifier("patPrincipalFactory") final PrincipalFactory patPrincipalFactory,
            @Qualifier("patPwPolicyConfiguration") final PasswordPolicyContext patPasswordPolicyConfiguration) {
        AbstractUsernamePasswordAuthenticationHandler authenticationHandler = new PATAuthenticationHandler("PAT", null, patPrincipalFactory, 1);
        authenticationHandler.setPasswordPolicyConfiguration(patPasswordPolicyConfiguration);
        LOGGER.info("The Handler for PAT has been called");
        return authenticationHandler;
    }

    @ConditionalOnMissingBean(name = "patPrincipalFactory")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PrincipalFactory patAuthenticationHandlerPrincipalFactory() {
        return PrincipalFactoryUtils.newPrincipalFactory();
    }

    @ConditionalOnMissingBean(name = "rejectUsersAuthenticationEventExecutionPlanConfigurer")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public AuthenticationEventExecutionPlanConfigurer configureAuthenticationExecutionPlan(
            @Qualifier("patAuthenticationHandler") final AuthenticationHandler patAuthenticationHandler) {
        return plan -> {
            plan.registerAuthenticationHandler(patAuthenticationHandler);
        };
    }


    @ConditionalOnMissingBean(name = "patPwPolicyConfiguration")
    @Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    public PasswordPolicyContext patPasswordPolicyConfiguration() {
        return new PasswordPolicyContext();
    }

}
