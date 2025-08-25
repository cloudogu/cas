package de.triology.cas.authentication;

import org.apereo.cas.authentication.handler.ByCredentialSourceAuthenticationHandlerResolver;
import org.apereo.cas.authentication.handler.DefaultAuthenticationHandlerResolver;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.DefaultAuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.apereo.cas.authentication.MultifactorAuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationException;

import org.apereo.cas.multitenancy.TenantExtractor;

@Slf4j
public class LegacyDefaultAuthenticationEventExecutionPlan extends DefaultAuthenticationEventExecutionPlan {

    private final Map<AuthenticationHandler, PrincipalResolver> authenticationHandlerPrincipalResolverMap = new LinkedHashMap<>(0);

    public LegacyDefaultAuthenticationEventExecutionPlan(
            AuthenticationHandlerResolver defaultAuthenticationHandlerResolver, TenantExtractor tenantExtractor) {
        super(defaultAuthenticationHandlerResolver, tenantExtractor);
    }

    /**
     * Only override the handler resolution. Everything else stays as-is.
     */
    @Override
    public @NonNull Set<AuthenticationHandler> resolveAuthenticationHandlers(final AuthenticationTransaction transaction) throws Throwable {
        // Use the public API of the parent
        val handlers = super.getAuthenticationHandlers();
        LOGGER.debug("Candidate/Registered authentication handlers for this transaction [{}] are [{}]", transaction, handlers);

        val handlerResolvers = super.getAuthenticationHandlerResolvers(transaction);
        LOGGER.debug("Authentication handler resolvers for this transaction are [{}]", handlerResolvers);

        val resolvedHandlers = handlerResolvers.stream()
            .filter(BeanSupplier::isNotProxy)
            .filter(Unchecked.predicate(r -> r.supports(handlers, transaction)))
            .map(Unchecked.function(r -> r.resolve(handlers, transaction)))
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        // Fallback to a fresh DefaultAuthenticationHandlerResolver instance
        if (resolvedHandlers.isEmpty()) {
            LOGGER.debug("Resolvers produced no candidates. Using the default resolver fallback...");
            var fallback = new DefaultAuthenticationHandlerResolver();
            if (fallback.supports(handlers, transaction)) {
                resolvedHandlers.addAll(fallback.resolve(handlers, transaction));
            }
        }

        // Optional: filter by credential source, but *don’t* assume access to parent internals
        val byCredential = new ByCredentialSourceAuthenticationHandlerResolver();
        if (byCredential.supports(resolvedHandlers, transaction)) {
            val credentialHandlers = byCredential.resolve(resolvedHandlers, transaction);
            if (!credentialHandlers.isEmpty()) {
                LOGGER.debug("Handlers resolved by credential source are [{}]", credentialHandlers);
                resolvedHandlers.removeIf(handler ->
                    !(handler instanceof MultifactorAuthenticationHandler)
                    && credentialHandlers.stream().noneMatch(credHandler ->
                        StringUtils.equalsIgnoreCase(credHandler.getName(), handler.getName())));
            }
        }

        if (resolvedHandlers.isEmpty()) {
            throw new AuthenticationException("No authentication handlers could be resolved to support the authentication transaction");
        }
        LOGGER.debug("Resolved and finalized authentication handlers for this transaction are [{}]", resolvedHandlers);
        return resolvedHandlers;
    }

    @Override
    public Set<AuthenticationHandler> getAuthenticationHandlers() {
        val handlers = authenticationHandlerPrincipalResolverMap.keySet().toArray(AuthenticationHandler[]::new);
        AnnotationAwareOrderComparator.sortIfNecessary(handlers);
        return new LinkedHashSet<>(CollectionUtils.wrapList(handlers));
    }
}