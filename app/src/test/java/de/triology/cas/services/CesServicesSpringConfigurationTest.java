package de.triology.cas.services;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlanConfigurer;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link CesServicesSpringConfiguration}.
 */
class CesServicesSpringConfigurationTest {

    private CesServicesSpringConfiguration configuration;
    private AuthenticationHandlerResolver defaultResolver;
    private TenantExtractor tenantExtractor;

    @BeforeEach
    void setUp() {
        configuration = new CesServicesSpringConfiguration();
        defaultResolver = mock(AuthenticationHandlerResolver.class);
        tenantExtractor = mock(TenantExtractor.class);
    }

    @Test
    void serviceMatchingStrategy_ReturnsCesServiceMatchingStrategy() {
        ServiceMatchingStrategy strategy = configuration.serviceMatchingStrategy();

        assertNotNull(strategy);
        assertInstanceOf(CesServiceMatchingStrategy.class, strategy);
    }

    @Test
    void authenticationEventExecutionPlan_WithNoConfigurers_ReturnsPlan() {
        AuthenticationEventExecutionPlan plan = configuration.authenticationEventExecutionPlan(
                defaultResolver, tenantExtractor, List.of());

        assertNotNull(plan);
    }

    @Test
    void authenticationEventExecutionPlan_InvokesEachConfigurer() throws Exception {
        AuthenticationEventExecutionPlanConfigurer configurer = mock(AuthenticationEventExecutionPlanConfigurer.class);

        AuthenticationEventExecutionPlan plan = configuration.authenticationEventExecutionPlan(
                defaultResolver, tenantExtractor, List.of(configurer));

        assertNotNull(plan);
        verify(configurer).configureAuthenticationExecutionPlan(plan);
    }

    @Test
    void authenticationEventExecutionPlan_SwallowsConfigurerException() throws Exception {
        AuthenticationEventExecutionPlanConfigurer failing = mock(AuthenticationEventExecutionPlanConfigurer.class);
        doThrow(new RuntimeException("boom")).when(failing).configureAuthenticationExecutionPlan(org.mockito.ArgumentMatchers.any());
        AuthenticationEventExecutionPlanConfigurer succeeding = mock(AuthenticationEventExecutionPlanConfigurer.class);

        assertDoesNotThrow(() -> {
            // Spring always injects a mutable list for multi-bean List<T> parameters (the bean
            // method sorts it in place); use one here too rather than the immutable List.of().
            AuthenticationEventExecutionPlan plan = configuration.authenticationEventExecutionPlan(
                    defaultResolver, tenantExtractor, new java.util.ArrayList<>(List.of(failing, succeeding)));
            verify(succeeding).configureAuthenticationExecutionPlan(plan);
        });
    }
}
