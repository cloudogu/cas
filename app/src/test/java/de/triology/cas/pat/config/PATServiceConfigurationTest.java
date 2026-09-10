package de.triology.cas.pat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

import javax.sql.DataSource;

import tools.jackson.databind.ObjectMapper;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.authentication.PATAuthenticationHandler;
import de.triology.cas.pat.config.persistence.PATDatabaseProvider;
import de.triology.cas.pat.controller.PATController;
import de.triology.cas.pat.controller.PATExceptionHandler;
import de.triology.cas.pat.controller.PATMethodNotAllowedHandler;
import de.triology.cas.pat.repository.JdbcPATRepository;
import de.triology.cas.pat.repository.PATRepository;
import de.triology.cas.pat.service.PATService;
import de.triology.cas.pat.service.SecurePATGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

class PATServiceConfigurationTest {
    private final PATServiceConfiguration configuration = new PATServiceConfiguration();

    @Test
    void selectsExactlyOneMatchingDatabaseProvider() {
        PATServiceProperties properties = properties("jdbc:test");
        DataSource dataSource = mock(DataSource.class);
        PATDatabaseProvider unsupported = provider(false, mock(DataSource.class));
        PATDatabaseProvider supported = provider(true, dataSource);

        assertSame(dataSource, configuration.patDataSource(properties, List.of(unsupported, supported)));
        verify(supported).createDataSource(properties);

        IllegalStateException none = assertThrows(IllegalStateException.class,
                () -> configuration.patDataSource(properties, List.of(unsupported)));
        IllegalStateException multiple = assertThrows(IllegalStateException.class,
                () -> configuration.patDataSource(properties, List.of(supported, supported)));
        assertEquals("Exactly one PAT database provider must support the configured database URL; found 0",
                none.getMessage());
        assertEquals("Exactly one PAT database provider must support the configured database URL; found 2",
                multiple.getMessage());
    }

    @Test
    void createsApplicationBeansWithProvidedDependencies() {
        DataSource dataSource = mock(DataSource.class);
        JdbcTemplate jdbcTemplate = configuration.patJdbcTemplate(dataSource);
        PATRepository repository = configuration.patRepository(jdbcTemplate);
        SecureRandom random = configuration.patSecureRandom();
        SecurePATGenerator generator = configuration.securePATGenerator(random);
        Clock clock = configuration.patClock();
        PATService service = configuration.patService(repository, generator, clock);

        assertSame(dataSource, jdbcTemplate.getDataSource());
        assertInstanceOf(JdbcPATRepository.class, repository);
        assertNotNull(random);
        assertNotNull(generator.generate());
        assertNotNull(clock.instant());
        assertNotNull(service);
        assertInstanceOf(PATController.class, configuration.patController(service));
        assertInstanceOf(PATExceptionHandler.class, configuration.patExceptionHandler(clock));
        assertInstanceOf(PATMethodNotAllowedHandler.class, configuration.patMethodNotAllowedHandler(clock));
        assertInstanceOf(PATSecurityHandlers.class,
                configuration.patSecurityHandlers(new ObjectMapper(), clock));
    }

    @Test
    void refusesSecurityChainWithoutExplicitBasicCredentials() {
        SecurityProperties securityProperties = new SecurityProperties();
        HttpSecurity http = mock(HttpSecurity.class);
        PATSecurityHandlers handlers = mock(PATSecurityHandlers.class);

        IllegalStateException generatedPassword = assertThrows(IllegalStateException.class,
                () -> configuration.patSecurityFilterChain(http, handlers, securityProperties));
        securityProperties.getUser().setName(" ");
        securityProperties.getUser().setPassword("configured-password");
        IllegalStateException missingName = assertThrows(IllegalStateException.class,
                () -> configuration.patSecurityFilterChain(http, handlers, securityProperties));
        securityProperties.getUser().setName("service");
        securityProperties.getUser().setPassword(" ");
        IllegalStateException missingPassword = assertThrows(IllegalStateException.class,
                () -> configuration.patSecurityFilterChain(http, handlers, securityProperties));

        assertEquals(generatedPassword.getMessage(), missingName.getMessage());
        assertEquals(generatedPassword.getMessage(), missingPassword.getMessage());
    }

    @Test
    void propertiesRoundTrip() {
        PATServiceProperties properties = properties("jdbc:sqlite:test.db");
        assertEquals(true, properties.isEnabled());
        assertEquals("jdbc:sqlite:test.db", properties.getDatabaseUrl());
    }

    @Test
    void createsAndRegistersPatAuthenticationHandler() throws Exception {
        PATService service = mock(PATService.class);
        CesGroupAwareLdapAuthenticationHandler ldapHandler =
                mock(CesGroupAwareLdapAuthenticationHandler.class);
        PrincipalResolver principalResolver = mock(PrincipalResolver.class);
        AuthenticationEventExecutionPlan plan = mock(AuthenticationEventExecutionPlan.class);

        PATAuthenticationHandler handler = configuration.patAuthenticationHandler(service, ldapHandler);
        var configurer = configuration.patAuthenticationEventExecutionPlanConfigurer(
                handler, principalResolver);
        configurer.configureAuthenticationExecutionPlan(plan);

        assertEquals("patAuthenticationHandler", handler.getName());
        verify(plan).registerAuthenticationHandlerWithPrincipalResolver(handler, principalResolver);
    }

    private static PATDatabaseProvider provider(boolean supports, DataSource dataSource) {
        PATDatabaseProvider provider = mock(PATDatabaseProvider.class);
        when(provider.supports("jdbc:test")).thenReturn(supports);
        when(provider.createDataSource(org.mockito.ArgumentMatchers.any())).thenReturn(dataSource);
        return provider;
    }

    private static PATServiceProperties properties(String url) {
        PATServiceProperties properties = new PATServiceProperties();
        properties.setEnabled(true);
        properties.setDatabaseUrl(url);
        return properties;
    }

    @Test
    void configuresFlywayForSelectedProvider() {
        var dataSource = mock(DataSource.class);
        var provider = provider(true, dataSource);
        when(provider.migrationLocation()).thenReturn("classpath:db/pat/migration/sqlite");
        var flyway = configuration.patFlyway(dataSource, properties("jdbc:test"), List.of(provider));
        assertSame(dataSource, flyway.getConfiguration().getDataSource());
        assertEquals("classpath:db/pat/migration/sqlite",
                flyway.getConfiguration().getLocations()[0].getDescriptor());
    }

    @Test
    void createsServiceTicketFactory() {
        assertInstanceOf(de.triology.cas.pat.authentication.PATServiceTicketFactory.class,
                configuration.defaultServiceTicketFactory(
                        mock(org.apereo.cas.ticket.tracking.TicketTrackingPolicy.class),
                        mock(org.apereo.cas.util.crypto.CipherExecutor.class),
                        mock(org.apereo.cas.ticket.ExpirationPolicyBuilder.class),
                        mock(org.apereo.cas.services.ServicesManager.class), java.util.Map.of(), mock(PATService.class)));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void refusesNullBasicCredentials(boolean missingUsername) {
        var properties = mock(SecurityProperties.class);
        var user = mock(SecurityProperties.User.class);
        when(properties.getUser()).thenReturn(user);
        if (!missingUsername) when(user.getName()).thenReturn("service");
        var http = mock(HttpSecurity.class);
        assertThrows(IllegalStateException.class,
                () -> configuration.patSecurityFilterChain(http, mock(PATSecurityHandlers.class), properties));
        org.mockito.Mockito.verifyNoInteractions(http);
    }

}
