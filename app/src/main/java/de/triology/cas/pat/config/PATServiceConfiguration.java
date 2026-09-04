package de.triology.cas.pat.config;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.triology.cas.pat.config.persistence.PATDatabaseProvider;
import de.triology.cas.pat.controller.PATController;
import de.triology.cas.pat.controller.PATExceptionHandler;
import de.triology.cas.pat.controller.PATMethodNotAllowedHandler;
import de.triology.cas.pat.repository.JdbcPATRepository;
import de.triology.cas.pat.repository.PATRepository;
import de.triology.cas.pat.service.PATService;
import de.triology.cas.pat.service.SecurePATGenerator;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Conditional Spring Boot auto-configuration for the complete PAT subsystem.
 * It owns a dedicated database data source, migration lifecycle, service graph and security chain.
 */
@AutoConfiguration
@EnableConfigurationProperties(PATServiceProperties.class)
@ConditionalOnProperty(prefix = "personal-acces-token-service", name = "enabled", havingValue = "true")
public class PATServiceConfiguration {
    private static final int TOKEN_RANDOM_BYTES = 32;

    /**
     * Creates the dedicated database data source.
     *
     * @param properties validated PAT configuration
     * @param databaseProviders available vendor-specific providers
     * @return configured PAT data source
     */
    @Bean(name = "patDataSource", defaultCandidate = false)
    public DataSource patDataSource(
            PATServiceProperties properties,
            List<PATDatabaseProvider> databaseProviders) {
        return findDatabaseProvider(properties, databaseProviders)
                .createDataSource(properties);
    }

    /**
     * Owns the single migration lifecycle for the PAT database. Not a default candidate.
     *
     * @param dataSource PAT data source
     * @param properties validated PAT configuration
     * @param databaseProviders available vendor-specific providers
     * @return configured PAT Flyway instance
     */
    @Bean(name = "patFlyway", defaultCandidate = false, initMethod = "migrate")
    public Flyway patFlyway(
            @Qualifier("patDataSource") DataSource dataSource,
            PATServiceProperties properties,
            List<PATDatabaseProvider> databaseProviders) {
        PATDatabaseProvider provider = findDatabaseProvider(properties, databaseProviders);
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(provider.migrationLocation())
                .load();
    }

    /**
     * Creates the JDBC template isolated to the PAT data source.
     *
     * @param dataSource PAT data source
     * @return PAT JDBC template
     */
    @Bean(name = "patJdbcTemplate", defaultCandidate = false)
    @DependsOn("patFlyway")
    public JdbcTemplate patJdbcTemplate(@Qualifier("patDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Creates the Spring JDBC PAT repository.
     *
     * @param jdbcTemplate PAT JDBC template
     * @return database-agnostic repository implementation
     */
    @Bean
    public PATRepository patRepository(@Qualifier("patJdbcTemplate") JdbcTemplate jdbcTemplate) {
        return new JdbcPATRepository(jdbcTemplate);
    }

    /**
     * Creates the cryptographically secure random source for token generation.
     *
     * @return secure random source
     */
    @Bean
    public SecureRandom patSecureRandom() {
        return new SecureRandom();
    }

    /**
     * Creates the configured high-entropy PAT generator.
     *
     * @param patSecureRandom secure random source
     * @return token generator
     */
    @Bean
    public SecurePATGenerator securePATGenerator(@Qualifier("patSecureRandom") SecureRandom patSecureRandom) {
        return new SecurePATGenerator(patSecureRandom, TOKEN_RANDOM_BYTES);
    }

    /**
     * Creates the shared clock for PAT success and error timestamps.
     *
     * @return UTC system clock
     */
    @Bean(name = "patClock", defaultCandidate = false)
    public Clock patClock() {
        return Clock.systemUTC();
    }

    /**
     * Creates the PAT application service.
     *
     * @param repository PAT repository
     * @param generator token generator
     * @param clock clock used for timestamps
     * @return PAT application service
     */
    @Bean
    public PATService patService(
            PATRepository repository,
            SecurePATGenerator generator,
            @Qualifier("patClock") Clock clock) {
        return new PATService(repository, generator, clock);
    }

    /**
     * Creates the PAT HTTP controller.
     *
     * @param service PAT application service
     * @return PAT controller
     */
    @Bean
    public PATController patController(PATService service) {
        return new PATController(service);
    }

    /**
     * Creates the PAT-specific structured exception handler.
     *
     * @param clock clock used for error timestamps
     * @return PAT exception handler
     */
    @Bean
    public PATExceptionHandler patExceptionHandler(@Qualifier("patClock") Clock clock) {
        return new PATExceptionHandler(clock);
    }

    /**
     * Creates PAT-specific handling for unsupported HTTP methods.
     *
     * @param clock clock used for error timestamps
     * @return method-not-allowed handler
     */
    @Bean
    public PATMethodNotAllowedHandler patMethodNotAllowedHandler(@Qualifier("patClock") Clock clock) {
        return new PATMethodNotAllowedHandler(clock);
    }

    /**
     * Creates JSON-producing Spring Security failure handlers.
     *
     * @param objectMapper application JSON mapper
     * @param clock clock used for error timestamps
     * @return PAT security handlers
     */
    @Bean
    public PATSecurityHandlers patSecurityHandlers(
            ObjectMapper objectMapper,
            @Qualifier("patClock") Clock clock) {
        return new PATSecurityHandlers(objectMapper, clock);
    }

    /**
     * Creates an isolated HTTP Basic security chain for PAT routes and disables CSRF only there.
     *
     * @param http Spring Security chain builder
     * @param handlers JSON authentication failure handler
     * @param securityProperties existing HTTP Basic credentials
     * @return configured PAT security filter chain
     * @throws Exception when Spring Security cannot build the chain
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 15)
    public SecurityFilterChain patSecurityFilterChain(
            HttpSecurity http, PATSecurityHandlers handlers, SecurityProperties securityProperties) throws Exception {
        validateSecurityUser(securityProperties);
        return http
                .securityMatcher("/api/users/*/pats", "/api/users/*/pats/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic
                        .authenticationEntryPoint(handlers))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(handlers))
                .build();
    }

    private PATDatabaseProvider findDatabaseProvider(
            PATServiceProperties properties,
            List<PATDatabaseProvider> databaseProviders) {
        List<PATDatabaseProvider> matchingProviders = databaseProviders.stream()
                .filter(provider -> provider.supports(properties.getDatabaseUrl()))
                .toList();
        if (matchingProviders.size() != 1) {
            throw new IllegalStateException(
                    "Exactly one PAT database provider must support the configured database URL; found "
                            + matchingProviders.size());
        }
        return matchingProviders.getFirst();
    }

    /**
     * Ensures that enabling PATs never falls back to generated development credentials.
     *
     * @param securityProperties bound Spring Security user settings
     * @throws IllegalStateException when username or password is absent
     */
    private void validateSecurityUser(SecurityProperties securityProperties) {
        SecurityProperties.User user = securityProperties.getUser();
        if (user.getName() == null || user.getName().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()
                || user.isPasswordGenerated()) {
            throw new IllegalStateException(
                    "spring.security.user.name and spring.security.user.password must be configured when the PAT service is enabled");
        }
    }
}
