package de.triology.cas.pat.config;

import java.time.Clock;
import java.lang.reflect.Proxy;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.Instant;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import de.triology.cas.ldap.CesGroupAwareLdapAuthenticationHandler;
import de.triology.cas.pat.authentication.PATAuthenticationHandler;
import de.triology.cas.pat.controller.PATPrincipalController;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.service.PATService;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PATPrincipalSecurityFilterChainTest {
    @Test
    void startsWithJdkProxiedHandler() {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            var handler = context.getBean("patAuthenticationHandler", AuthenticationHandler.class);
            assertTrue(Proxy.isProxyClass(handler.getClass()));
            assertFalse(handler instanceof PATAuthenticationHandler);
            assertNotNull(context.getBean(SecurityFilterChain.class));
        }
    }

    @Test
    void authenticatesWithoutPriorLoginAndRevalidatesEveryRequest() throws Throwable {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            var service = context.getBean(PATService.class);
            var ldap = context.getBean(CesGroupAwareLdapAuthenticationHandler.class);
            when(service.resolve("pat_secret")).thenReturn(Optional.of(new PATMetadata(
                    UUID.randomUUID(), "alice", "test", Instant.EPOCH, null, "/redmine")));
            when(ldap.resolvePrincipal("alice")).thenReturn(PrincipalFactoryUtils.newPrincipalFactory()
                    .createPrincipal("alice", Map.of("mail", List.of("alice@example.test"))));
            when(service.isScopeAllowed("/redmine", "/redmine")).thenCallRealMethod();
            var request = request("alice:pat_secret");
            var response = new MockHttpServletResponse();
            var reached = new AtomicBoolean();
            context.getBean(FilterChainProxy.class).doFilter(request, response, (req, res) -> {
                reached.set(true);
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                assertNull(authentication.getCredentials());
                var result = new PATPrincipalController(service).validate(authentication, "/redmine");
                assertEquals("alice", result.getBody().id());
                assertEquals(List.of("/redmine"), result.getBody().attributes().get("patScope"));
                assertEquals(List.of("alice@example.test"), result.getBody().attributes().get("mail"));
                assertEquals("no-store", result.getHeaders().getCacheControl());
            });
            assertTrue(reached.get());
            assertEquals(200, response.getStatus());
            assertNull(request.getSession(false));
            when(service.resolve("pat_secret")).thenReturn(Optional.empty());
            assertRejected(context, request("alice:pat_secret"));
            verify(service, times(2)).resolve("pat_secret");
        }
    }

    @Test
    void rejectsPasswordsWrongOwnersAndMissingOrMalformedCredentialsEvenWithSession() throws Throwable {
        try (var context = new AnnotationConfigApplicationContext(SecurityConfiguration.class)) {
            assertRejected(context, request(null));
            assertRejected(context, request("service:password"));
            assertRejected(context, request("alice:pat_unknown"));
            var malformed = request(null);
            malformed.addHeader("Authorization", "Basic !!!");
            assertRejected(context, malformed);
            var bearer = request(null);
            bearer.addHeader("Authorization", "Bearer pat_secret");
            assertRejected(context, bearer);
            var sessionRequest = request(null);
            var session = new MockHttpSession();
            session.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(
                    UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of())));
            sessionRequest.setSession(session);
            assertRejected(context, sessionRequest);
            when(context.getBean(PATService.class).resolve("pat_secret")).thenReturn(Optional.of(
                    new PATMetadata(UUID.randomUUID(), "alice", "test", Instant.EPOCH, null, "/*")));
            assertRejected(context, request("bob:pat_secret"));
            verifyNoInteractions(context.getBean(CesGroupAwareLdapAuthenticationHandler.class));
        }
    }

    private static void assertRejected(AnnotationConfigApplicationContext context,
                                       MockHttpServletRequest request) throws Exception {
        var response = new MockHttpServletResponse();
        context.getBean(FilterChainProxy.class).doFilter(request, response,
                (req, res) -> fail("Invalid credentials reached the endpoint"));
        assertEquals(401, response.getStatus());
        assertEquals("Basic realm=\"PAT API\"", response.getHeader("WWW-Authenticate"));
        assertNull(response.getRedirectedUrl());
    }

    private static MockHttpServletRequest request(String credentials) {
        var request = new MockHttpServletRequest("GET", "/api/pats/validate");
        request.setServletPath("/api/pats/validate");
        if (credentials != null) {
            request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8)));
        }
        return request;
    }

    @Configuration
    @EnableWebSecurity
    static class SecurityConfiguration {
        @Bean PATService service() { return mock(PATService.class); }
        @Bean CesGroupAwareLdapAuthenticationHandler ldap() {
            return mock(CesGroupAwareLdapAuthenticationHandler.class);
        }
        @Bean AuthenticationHandler patAuthenticationHandler(PATService service,
                                                              CesGroupAwareLdapAuthenticationHandler ldap) {
            var target = new PATAuthenticationHandler("pat", PrincipalFactoryUtils.newPrincipalFactory(),
                    0, service, ldap);
            var proxy = new ProxyFactory();
            proxy.setTarget(target);
            proxy.setInterfaces(AuthenticationHandler.class);
            return (AuthenticationHandler) proxy.getProxy();
        }
        @Bean SecurityFilterChain chain(HttpSecurity http,
                @Qualifier("patAuthenticationHandler") AuthenticationHandler handler) throws Exception {
            var errors = new PATSecurityHandlers(JsonMapper.builder().findAndAddModules().build(), Clock.systemUTC());
            return new PATServiceConfiguration().patPrincipalSecurityFilterChain(http, errors, handler);
        }
    }
}
