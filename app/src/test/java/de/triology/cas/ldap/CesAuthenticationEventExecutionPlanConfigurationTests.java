package de.triology.cas.ldap;

import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CesAuthenticationEventExecutionPlanConfiguration}.
 */
class CesAuthenticationEventExecutionPlanConfigurationTests {

    private CesAuthenticationEventExecutionPlanConfiguration configuration;
    private AuthenticationHandler authenticationHandler;

    @BeforeEach
    void setUp() {
        authenticationHandler = mock(AuthenticationHandler.class);
        configuration = new CesAuthenticationEventExecutionPlanConfiguration(authenticationHandler);
    }

    @Test
    void configureAuthenticationExecutionPlan_ShouldRegisterAuthenticationHandler() {
        // given
        AuthenticationEventExecutionPlan plan = mock(AuthenticationEventExecutionPlan.class);

        // when
        configuration.configureAuthenticationExecutionPlan(plan);

        // then
        verify(plan, times(1)).registerAuthenticationHandler(authenticationHandler);
    }

    @Test
    void constructor_ShouldCreateInstance() {
        // when / then
        assertNotNull(configuration, "Configuration instance should be created successfully");
    }
}
