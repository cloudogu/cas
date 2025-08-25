package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ChainingServicesManager;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.mgmt.DefaultChainingServicesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import de.triology.cas.authentication.LegacyDefaultAuthenticationEventExecutionPlan;

import java.util.List;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.core.annotation.Order;


import org.apereo.cas.authentication.AuthenticationHandler;
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
        List<AuthenticationEventExecutionPlanConfigurer> configurers // Spring injects empty list if none
    ) {
        var plan = new LegacyDefaultAuthenticationEventExecutionPlan();

        for (var c : configurers) {
            try {
                c.configureAuthenticationExecutionPlan(plan);
            } catch (Exception ex) {
                // Don't fail the whole context; just log which configurer failed.
                LOGGER.error("Failed to apply AuthenticationEventExecutionPlanConfigurer [{}]: {}",
                    c.getClass().getName(), ex.getMessage(), ex);
            }
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


    // @Bean
    // @Order(1)
    // SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    //     http
    //         // context path (/cas) is already stripped, so just match /actuator/**
    //         .securityMatcher("/actuator/**")
    //         .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
    //         .csrf(csrf -> csrf.ignoringRequestMatchers("/actuator/**"))
    //         .requestCache(cache -> cache.disable());
    //     return http.build();
    // }

    // @Bean({"servicesManager", "chainingServicesManager", "defaultChainingServicesManager"})
    // @Primary
    // public org.apereo.cas.services.ChainingServicesManager servicesManager() {
    //     return new SafeChainingServicesManager(); // returns as interface
    // }

    // @Component
    // class ServicesManagerAuditor {
    //     @Autowired private org.springframework.context.ApplicationContext ctx;

    //     @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    //     public void audit() {
    //         var beans = ctx.getBeansOfType(org.apereo.cas.services.ServicesManager.class);
    //         LOGGER.warn("[Audit] Found {} ServicesManager beans: {}", beans.size(), beans.keySet());
    //         beans.forEach((name, bean) -> {
    //         Class<?> target = org.springframework.aop.support.AopUtils.getTargetClass(bean);
    //         LOGGER.warn("[Audit] bean='{}', beanClass='{}', target='{}'",
    //             name, bean.getClass().getName(), target == null ? "?" : target.getName());
    //         try {
    //             var c = bean.load();
    //             LOGGER.warn("[Audit] {}#load(): {}", name, (c == null ? "NULL (BUG)" : c.size() + " services"));
    //         } catch (Exception ex) {
    //             LOGGER.error("[Audit] {} threw: {}", name, ex.getMessage(), ex);
    //         }
    //         });
    //     }
    // }

    // @Component
    // class ChainWireUp {
    // private final org.apereo.cas.services.ChainingServicesManager chain; // <-- interface
    // private final org.springframework.context.ApplicationContext ctx;

    // ChainWireUp(org.apereo.cas.services.ChainingServicesManager chain,
    //             org.springframework.context.ApplicationContext ctx) {
    //     this.chain = chain;
    //     this.ctx = ctx;
    // }

    // @org.springframework.context.event.EventListener(org.springframework.context.event.ContextRefreshedEvent.class)
    // public void wireChildren() {
    //     var beans = ctx.getBeansOfType(org.apereo.cas.services.ServicesManager.class);

    //     // Prefer comparing bean names, not instances (proxies!)
    //     beans.forEach((name, mgr) -> {
    //     if ("servicesManager".equals(name)) return; // skip the chain itself
    //     chain.registerServiceManager(mgr);
    //     LOGGER.warn("[Ces] Registered child ServicesManager '{}' ({}) into SafeChain",
    //         name, mgr.getClass().getName());
    //     });
    //     LOGGER.warn("[Ces] SafeChain now has {} child manager(s)",
    //         chain.getServiceManagers().size());
    // }
    // }
}
