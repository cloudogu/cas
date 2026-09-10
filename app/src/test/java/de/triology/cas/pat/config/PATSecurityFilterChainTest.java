package de.triology.cas.pat.config;

import java.time.Clock;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class PATSecurityFilterChainTest {
    @Test
    void requiresBasicAuthenticationAndAllowsStatelessPostWithoutCsrf() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            var proxy = context.getBean(FilterChainProxy.class);
            var response = new MockHttpServletResponse();
            var reachedEndpoint = new AtomicBoolean();
            proxy.doFilter(request(), response, (req, res) -> reachedEndpoint.set(true));
            assertEquals(401, response.getStatus());
            assertFalse(reachedEndpoint.get());
            assertEquals("Basic realm=\"PAT API\"", response.getHeader("WWW-Authenticate"));

            var authenticated = request();
            authenticated.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                    "service:password".getBytes(StandardCharsets.UTF_8)));
            var success = new MockHttpServletResponse();
            proxy.doFilter(authenticated, success, (req, res) -> reachedEndpoint.set(true));
            assertTrue(reachedEndpoint.get());
            assertEquals(200, success.getStatus());
            assertNull(authenticated.getSession(false));

            var unrelated = new MockHttpServletRequest("GET", "/login");
            unrelated.setServletPath("/login");
            assertFalse(context.getBean(SecurityFilterChain.class).matches(unrelated));
        }
    }

    private static MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("POST", "/api/users/alice/pats");
        request.setServletPath("/api/users/alice/pats");
        return request;
    }

    @Configuration
    @EnableWebSecurity
    static class SecurityConfiguration {
        @Bean
        InMemoryUserDetailsManager users() {
            return new InMemoryUserDetailsManager(User.withUsername("service")
                    .password("{noop}password").roles("USER").build());
        }

        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            var properties = new SecurityProperties();
            properties.getUser().setName("service");
            properties.getUser().setPassword("password");
            var handlers = new PATSecurityHandlers(JsonMapper.builder().findAndAddModules().build(), Clock.systemUTC());
            return new PATServiceConfiguration().patSecurityFilterChain(http, handlers, properties);
        }
    }
}
