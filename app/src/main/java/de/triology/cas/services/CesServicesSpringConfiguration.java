package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import de.triology.cas.authentication.LegacyDefaultAuthenticationEventExecutionPlan;
import java.util.List;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.principal.PrincipalResolver;

@Configuration("CesServicesSpringConfiguration")
@ComponentScan("de.triology.cas.services")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesServicesSpringConfiguration {

    @Bean(name = "serviceMatchingStrategy")
    public ServiceMatchingStrategy serviceMatchingStrategy() {
        return new CesServiceMatchingStrategy();
    }

    @Bean
    @Primary
    public AuthenticationEventExecutionPlan authenticationEventExecutionPlan(
        AuthenticationHandlerResolver defaultResolver,
        TenantExtractor tenantExtractor,
        List<AuthenticationEventExecutionPlanConfigurer> configurers
    ) {
        var plan = new LegacyDefaultAuthenticationEventExecutionPlan(defaultResolver, tenantExtractor);
        org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(configurers);
        for (var c : configurers) {
            try { c.configureAuthenticationExecutionPlan(plan); }
            catch (Exception ex) { LOGGER.error("Configurer {} failed", c.getClass().getName(), ex); }
        }
        return plan;
    }

    @Bean
    public AuthenticationEventExecutionPlanConfigurer cesAuthPlanConfigurer(
        @Qualifier("cesGroupAwareLdapAuthenticationHandler")
        AuthenticationHandler ldapHandler,
        @Qualifier(PrincipalResolver.BEAN_NAME_PRINCIPAL_RESOLVER)
        PrincipalResolver principalResolver
    ) {
        return plan -> {
            plan.registerAuthenticationHandlerWithPrincipalResolver(ldapHandler, principalResolver);
            // add more handlers here if you want, but qualify them explicitly
        };
    }
}
