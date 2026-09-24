package de.triology.cas.authentication;

import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyDefaultAuthenticationEventExecutionPlanTest {

    @Test
    void resolveAuthenticationHandlers_ThrowsWhenNothingCanBeResolved() throws Throwable {
        // No registered handler resolvers, no registered handlers, no credentials: every resolution
        // strategy (stream of resolvers, the DefaultAuthenticationHandlerResolver fallback, and
        // ByCredentialSourceAuthenticationHandlerResolver) has nothing to work with, so the method
        // must throw rather than return an empty set.
        AuthenticationHandlerResolver defaultResolver = mock(AuthenticationHandlerResolver.class);
        TenantExtractor tenantExtractor = mock(TenantExtractor.class);
        AuthenticationTransaction transaction = mock(AuthenticationTransaction.class);
        when(transaction.getCredentials()).thenReturn(List.of());

        var plan = new LegacyDefaultAuthenticationEventExecutionPlan(defaultResolver, tenantExtractor);

        assertThrows(AuthenticationException.class, () -> plan.resolveAuthenticationHandlers(transaction));
    }

    @Test
    void rejectsPatFromLoginPathIncludingProxyPrefix() throws Throwable {
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/auth/login");
        request.setServletPath("/auth/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            AuthenticationTransaction transaction = mock(AuthenticationTransaction.class);
            when(transaction.getCredentials()).thenReturn(List.of(
                    new org.apereo.cas.authentication.credential.UsernamePasswordCredential("alice", "pat_secret")));

            var plan = new LegacyDefaultAuthenticationEventExecutionPlan(
                    mock(AuthenticationHandlerResolver.class), mock(TenantExtractor.class));

            var exception = assertThrows(AuthenticationException.class,
                    () -> plan.resolveAuthenticationHandlers(transaction));
            assertEquals("Personal access tokens are not allowed for web login", exception.getMessage());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void allowsNormalPasswordAtLoginToContinueWithRegularResolution() throws Throwable {
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            AuthenticationTransaction transaction = mock(AuthenticationTransaction.class);
            when(transaction.getCredentials()).thenReturn(List.of(
                    new org.apereo.cas.authentication.credential.UsernamePasswordCredential("alice", "normal-password")));

            var plan = new LegacyDefaultAuthenticationEventExecutionPlan(
                    mock(AuthenticationHandlerResolver.class), mock(TenantExtractor.class));

            assertThrows(AuthenticationException.class, () -> plan.resolveAuthenticationHandlers(transaction));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void ignoresNonUsernamePasswordCredentialsAtLogin() throws Throwable {
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        try {
            AuthenticationTransaction transaction = mock(AuthenticationTransaction.class);
            when(transaction.getCredentials()).thenReturn(List.of(mock(org.apereo.cas.authentication.Credential.class)));

            var plan = new LegacyDefaultAuthenticationEventExecutionPlan(
                    mock(AuthenticationHandlerResolver.class), mock(TenantExtractor.class));

            assertThrows(AuthenticationException.class, () -> plan.resolveAuthenticationHandlers(transaction));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void resolveAuthenticationHandlers_ReturnsHandlersResolvedByRegisteredResolver() throws Throwable {
        AuthenticationHandlerResolver defaultResolver = mock(AuthenticationHandlerResolver.class);
        TenantExtractor tenantExtractor = mock(TenantExtractor.class);
        AuthenticationTransaction transaction = mock(AuthenticationTransaction.class);
        // No credentials: ByCredentialSourceAuthenticationHandlerResolver.supports() will find
        // nothing to match against, so it will not filter out the handler resolved below.
        when(transaction.getCredentials()).thenReturn(List.of());

        AuthenticationHandler handlerA = mock(AuthenticationHandler.class);
        when(handlerA.getName()).thenReturn("handlerA");

        AuthenticationHandlerResolver customResolver = mock(AuthenticationHandlerResolver.class);
        when(customResolver.supports(any(), any())).thenReturn(true);
        when(customResolver.resolve(any(), any())).thenReturn(Set.of(handlerA));

        var plan = new LegacyDefaultAuthenticationEventExecutionPlan(defaultResolver, tenantExtractor);
        plan.registerAuthenticationHandlerResolver(customResolver);

        Set<AuthenticationHandler> result = plan.resolveAuthenticationHandlers(transaction);

        assertEquals(1, result.size());
        assertTrue(result.contains(handlerA));
    }
}
