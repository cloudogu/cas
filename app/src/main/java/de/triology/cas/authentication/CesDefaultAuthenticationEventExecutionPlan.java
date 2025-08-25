package de.triology.cas.authentication;

import org.apereo.cas.authentication.handler.ByCredentialSourceAuthenticationHandlerResolver;
import org.apereo.cas.authentication.handler.DefaultAuthenticationHandlerResolver;
import org.apereo.cas.authentication.principal.PrincipalResolver;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.spring.beans.BeanSupplier;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationHandlerResolver;
import org.apereo.cas.authentication.DefaultAuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationMetaDataPopulator;
import org.apereo.cas.authentication.AuthenticationPostProcessor;
import org.apereo.cas.authentication.AuthenticationPreProcessor;
import org.apereo.cas.authentication.AuthenticationPolicy;
import org.apereo.cas.authentication.AuthenticationPolicyResolver;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationTransaction;
import org.apereo.cas.authentication.MultifactorAuthenticationHandler;
import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationException;

import org.apereo.cas.multitenancy.TenantExtractor;
import lombok.RequiredArgsConstructor;

import lombok.Getter;
import org.apereo.inspektr.common.web.ClientInfoHolder;
import java.util.Objects;


/**
 * This is {@link DefaultAuthenticationEventExecutionPlan}.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
@RequiredArgsConstructor
@Accessors(chain = true)
public class CesDefaultAuthenticationEventExecutionPlan implements AuthenticationEventExecutionPlan {
    private final List<AuthenticationMetaDataPopulator> authenticationMetaDataPopulatorList = new ArrayList<>();

    private final List<AuthenticationPostProcessor> authenticationPostProcessors = new ArrayList<>();

    private final List<AuthenticationPreProcessor> authenticationPreProcessors = new ArrayList<>();

    private final List<AuthenticationPolicy> authenticationPolicies = new ArrayList<>();

    private final List<AuthenticationHandlerResolver> authenticationHandlerResolvers = new ArrayList<>();

    private final List<AuthenticationPolicyResolver> authenticationPolicyResolvers = new ArrayList<>();

    private final Map<AuthenticationHandler, PrincipalResolver> authenticationHandlerPrincipalResolverMap = new LinkedHashMap<>();

    private final AuthenticationHandlerResolver defaultAuthenticationHandlerResolver;

    @Getter
    private final TenantExtractor tenantExtractor;

    @Override
    public boolean registerAuthenticationHandler(final AuthenticationHandler handler) {
        return registerAuthenticationHandlerWithPrincipalResolver(handler, null);
    }

    @Override
    public void registerAuthenticationMetadataPopulator(final AuthenticationMetaDataPopulator populator) {
        if (BeanSupplier.isNotProxy(populator)) {
            LOGGER.trace("Registering metadata populator [{}] into the execution plan", populator);
            authenticationMetaDataPopulatorList.add(populator);
        }
    }

    @Override
    public void registerAuthenticationPostProcessor(final AuthenticationPostProcessor processor) {
        if (BeanSupplier.isNotProxy(processor)) {
            LOGGER.warn("Registering authentication post processor [{}] into the execution plan", processor);
            authenticationPostProcessors.add(processor);
        }
    }

    @Override
    public void registerAuthenticationPreProcessor(final AuthenticationPreProcessor processor) {
        if (BeanSupplier.isNotProxy(processor)) {
            LOGGER.warn("Registering authentication pre processor [{}] into the execution plan", processor);
            authenticationPreProcessors.add(processor);
        }
    }

    @Override
    public void registerAuthenticationMetadataPopulators(final Collection<AuthenticationMetaDataPopulator> populators) {
        populators.stream().filter(BeanSupplier::isNotProxy).forEach(this::registerAuthenticationMetadataPopulator);
    }

    @Override
    public void registerAuthenticationPolicy(final AuthenticationPolicy authenticationPolicy) {
        if (BeanSupplier.isNotProxy(authenticationPolicy)) {
            this.authenticationPolicies.add(authenticationPolicy);
        }
    }

    @Override
    public void registerAuthenticationPolicies(final Collection<AuthenticationPolicy> authenticationPolicy) {
        this.authenticationPolicies.addAll(authenticationPolicy.stream().filter(BeanSupplier::isNotProxy).toList());
    }

    @Override
    public void registerAuthenticationHandlerResolver(final AuthenticationHandlerResolver handlerResolver) {
        if (BeanSupplier.isNotProxy(handlerResolver)) {
            this.authenticationHandlerResolvers.add(handlerResolver);
        }
    }

    @Override
    public void registerAuthenticationPolicyResolver(final AuthenticationPolicyResolver policyResolver) {
        if (BeanSupplier.isNotProxy(policyResolver)) {
            this.authenticationPolicyResolvers.add(policyResolver);
        }
    }

    @Override
    public void registerAuthenticationHandlerWithPrincipalResolver(final Map<AuthenticationHandler, PrincipalResolver> plan) {
        plan.forEach(this::registerAuthenticationHandlerWithPrincipalResolver);
    }

    @Override
    public boolean registerAuthenticationHandlerWithPrincipalResolver(final AuthenticationHandler handler,
                                                                      final PrincipalResolver principalResolver) {
        return FunctionUtils.doIf(BeanSupplier.isNotProxy(handler), () -> {
            LOGGER.trace("Registering handler [{}] with [{}] principal resolver into the execution plan",
                handler.getName(), Optional.ofNullable(principalResolver).map(PrincipalResolver::getName).orElse("no"));

            if (authenticationHandlerPrincipalResolverMap.containsKey(handler)) {
                LOGGER.error("Authentication execution plan has found an existing handler [{}]. "
                        + "Attempts to register a new authentication handler with the same name may lead to unpredictable results. "
                        + "Please make sure all authentication handlers are uniquely defined/named in the CAS configuration.",
                    handler.getName());
                return false;
            }
            authenticationHandlerPrincipalResolverMap.put(handler, principalResolver);
            return true;
        }, () -> false).get();
    }

    @Override
    public void registerAuthenticationHandlersWithPrincipalResolver(final Collection<AuthenticationHandler> handlers,
                                                                    final PrincipalResolver principalResolver) {
        handlers.stream().filter(BeanSupplier::isNotProxy)
            .forEach(h -> registerAuthenticationHandlerWithPrincipalResolver(h, principalResolver));
    }

    @Override
    public void registerAuthenticationHandlersWithPrincipalResolver(final List<AuthenticationHandler> handlers,
                                                                    final List<PrincipalResolver> principalResolver) {
        if (handlers.size() != principalResolver.size()) {
            LOGGER.error("Total number of authentication handlers must match the number of provided principal resolvers");
            return;
        }
        IntStream.range(0, handlers.size())
            .forEach(i -> registerAuthenticationHandlerWithPrincipalResolver(handlers.get(i), principalResolver.get(i)));
    }

    @Override
    public @NonNull Set<AuthenticationHandler> resolveAuthenticationHandlers(final AuthenticationTransaction transaction) throws Throwable {
        val handlers = getAuthenticationHandlers();
        LOGGER.warn("Candidate/Registered authentication handlers for this transaction [{}] are [{}]", transaction, handlers);
        val handlerResolvers = getAuthenticationHandlerResolvers(transaction);
        LOGGER.warn("Authentication handler resolvers for this transaction are [{}]", handlerResolvers);

        val resolvedHandlers = handlerResolvers
            .stream()
            .filter(BeanSupplier::isNotProxy)
            .filter(Unchecked.predicate(r -> r.supports(handlers, transaction)))
            .map(Unchecked.function(r -> r.resolve(handlers, transaction)))
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (resolvedHandlers.isEmpty()) {
            LOGGER.warn("Authentication handler resolvers produced no candidate authentication handler. Using the default handler resolver instead...");
            if (defaultAuthenticationHandlerResolver.supports(handlers, transaction)) {
                resolvedHandlers.addAll(defaultAuthenticationHandlerResolver.resolve(handlers, transaction));
            }
        }

        val byCredential = new ByCredentialSourceAuthenticationHandlerResolver();
        if (byCredential.supports(resolvedHandlers, transaction)) {
            val credentialHandlers = byCredential.resolve(resolvedHandlers, transaction);
            if (!credentialHandlers.isEmpty()) {
                LOGGER.warn("Authentication handlers resolved by credential source are [{}]", credentialHandlers);
                resolvedHandlers.removeIf(handler -> !(handler instanceof MultifactorAuthenticationHandler)
                    && credentialHandlers.stream().noneMatch(credHandler -> StringUtils.equalsIgnoreCase(credHandler.getName(), handler.getName())));
            }
        }

        if (resolvedHandlers.isEmpty()) {
            throw new AuthenticationException("No authentication handlers could be resolved to support the authentication transaction");
        }
        LOGGER.warn("Resolved and finalized authentication handlers to carry out this authentication transaction are [{}]", handlerResolvers);
        return resolvedHandlers;
    }

    @Override
    public Set<AuthenticationHandler> getAuthenticationHandlers() {
        val handlers = authenticationHandlerPrincipalResolverMap.keySet().toArray(AuthenticationHandler[]::new);
        AnnotationAwareOrderComparator.sortIfNecessary(handlers);
        return new LinkedHashSet<>(CollectionUtils.wrapList(handlers));
    }

    @Override
    public Collection<AuthenticationMetaDataPopulator> getAuthenticationMetadataPopulators(final AuthenticationTransaction transaction) {
        val list = new ArrayList<>(this.authenticationMetaDataPopulatorList);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.warn("Sorted and registered metadata populators for this transaction are [{}]", list);
        return list;
    }

    @Override
    public Collection<AuthenticationPostProcessor> getAuthenticationPostProcessors(final AuthenticationTransaction transaction) {
        val list = new ArrayList<>(this.authenticationPostProcessors);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Sorted and registered authentication post processors for this transaction are [{}]", list);
        return list;
    }

    @Override
    public Collection<AuthenticationPreProcessor> getAuthenticationPreProcessors(final AuthenticationTransaction transaction) {
        val list = new ArrayList<>(this.authenticationPreProcessors);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Sorted and registered authentication pre processors for this transaction are [{}]", list);
        return list;
    }

    @Override
    public PrincipalResolver getPrincipalResolver(final AuthenticationHandler handler,
                                                  final AuthenticationTransaction transaction) {
        return authenticationHandlerPrincipalResolverMap.get(handler);
    }

    @Override
    public Collection<AuthenticationPolicy> getAuthenticationPolicies(final AuthenticationTransaction transaction) {
        val handlerResolvers = getAuthenticationPolicyResolvers(transaction);
        LOGGER.warn("Authentication policy resolvers for this transaction are [{}]", handlerResolvers);

        val list = getAuthenticationPolicies();
        val resolvedPolicies = handlerResolvers.stream()
            .filter(Unchecked.predicate(r -> r.supports(transaction)))
            .map(Unchecked.function(r -> r.resolve(transaction)))
            .flatMap(Set::stream)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (resolvedPolicies.isEmpty()) {
            LOGGER.warn("Authentication policy resolvers produced no candidate authentication policy. Using default policies");
            return list;
        }
        LOGGER.warn("Resolved authentication policies are [{}]", resolvedPolicies);
        return resolvedPolicies;
    }

    @Override
    public Collection<AuthenticationPolicy> getAuthenticationPolicies(final Authentication authentication) {
        val list = new ArrayList<>(this.authenticationPolicies);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Sorted and registered authentication policies for this assertion are [{}]", list);
        return list;
    }

    @Override
    public Collection<AuthenticationPolicy> getAuthenticationPolicies() {
        val list = new ArrayList<>(this.authenticationPolicies);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Candidate authentication policies for this transaction are [{}]", list);
        return list;
    }

    @Override
    public Collection<AuthenticationHandlerResolver> getAuthenticationHandlerResolvers(final AuthenticationTransaction transaction) {
        val list = new ArrayList<>(this.authenticationHandlerResolvers);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Sorted and registered authentication handler resolvers for this transaction are [{}]", list);
        return list;
    }

    @Override
    public Collection<AuthenticationPolicyResolver> getAuthenticationPolicyResolvers(final AuthenticationTransaction transaction) {
        val list = new ArrayList<>(this.authenticationPolicyResolvers);
        AnnotationAwareOrderComparator.sort(list);
        LOGGER.trace("Sorted and registered authentication policy resolvers for this transaction are [{}]", list);
        return list;
    }
}