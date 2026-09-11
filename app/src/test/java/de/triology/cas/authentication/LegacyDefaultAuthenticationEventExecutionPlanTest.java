package de.triology.cas.authentication;

import org.apereo.cas.authentication.AuthenticationException;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.apereo.cas.multitenancy.TenantExtractor;
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
