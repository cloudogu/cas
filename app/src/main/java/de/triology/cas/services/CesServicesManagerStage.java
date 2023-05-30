package de.triology.cas.services;

import de.triology.cas.oidc.services.ProxyPolicySetter;
import de.triology.cas.services.attributes.ReturnMappedAttributesPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.services.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract class that encapsulates the stages {@link CesServicesManager} operates in.
 * It provides it's registered services via {@link #getRegisteredServices()}.
 * <p>
 * Implementations must initialize their registered services by implementing the template method
 * {@link #initRegisteredServices()}.
 */
@Slf4j
abstract class CesServicesManagerStage {

    private final CesServiceManagerConfiguration managerConfig;

    /**
     * Map to store all registeredServices.
     */
    protected final Map<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    CesServicesManagerStage(CesServiceManagerConfiguration managerConfig) {
        this.managerConfig = managerConfig;
    }


    /**
     * @return the services registered in this stage.
     */
    public Map<Long, RegisteredService> getRegisteredServices() {
        if (this.registeredServices.isEmpty()) {
            initRegisteredServices();
        }
        return this.registeredServices;
    }

    /**
     * Template method that add the stage-specific services to the <code>registeredServices</code>.
     */
    protected abstract void initRegisteredServices();


    protected abstract void updateRegisteredServices();

    /**
     * Registers a new service
     *
     * @param service service object to register
     */
    protected void addNewService(BaseRegisteredService service) {
        RegisteredServiceProxyPolicy proxyPolicy = new RegexMatchingRegisteredServiceProxyPolicy().setPattern("^https?://.*");

        if(service instanceof ProxyPolicySetter) {
            ((ProxyPolicySetter)service).setProxyPolicy(proxyPolicy);
        }

        service.setEvaluationOrder((int) service.getId());
        service.setAttributeReleasePolicy(new ReturnMappedAttributesPolicy(managerConfig.getAllowedAttributes(), managerConfig.getAttributesMappingRules()));
        service.setMatchingStrategy(new CesRegisteredServiceMatchingStrategy());

        if (managerConfig.isOidcAuthenticationDelegationEnabled()) {
            configureOidcDelegationService(service);
        }

        LOGGER.debug("Adding new service to service manager {}", service);
        registeredServices.put(service.getId(), service);
    }

    /**
     * Applies basic configuration to all services when the OIDC delegation authentication is enabled.
     *
     * @param service The service that should be configured
     */
    private void configureOidcDelegationService(BaseRegisteredService service) {
        if (managerConfig.getOidcPrincipalsAttribute() != null && !managerConfig.getOidcPrincipalsAttribute().isEmpty()) {
            var principalProvider = new PrincipalAttributeRegisteredServiceUsernameProvider();
            principalProvider.setUsernameAttribute(managerConfig.getOidcPrincipalsAttribute());
            service.setUsernameAttributeProvider(principalProvider);
        }

        List<String> allowedProviders = new ArrayList<>();
        allowedProviders.add(managerConfig.getOidcClientDisplayName());
        var delegatedAuthenticationPolicy = new DefaultRegisteredServiceDelegatedAuthenticationPolicy();
        delegatedAuthenticationPolicy.setAllowedProviders(allowedProviders);
        var accessStrategy = new DefaultRegisteredServiceAccessStrategy();
        accessStrategy.setDelegatedAuthenticationPolicy(delegatedAuthenticationPolicy);
        service.setAccessStrategy(accessStrategy);
    }

    /**
     * @return a new numeric ID for a registered service
     */
    protected long createId() {
        return findHighestId(registeredServices) + 1;
    }

    /**
     * @return the highest number within the keyset of <code>map</code>
     */
    private static long findHighestId(Map<Long, RegisteredService> map) {
        long id = 0;

        for (Map.Entry<Long, RegisteredService> entry : map.entrySet()) {
            if (entry.getKey() > id) {
                id = entry.getKey();
            }
        }
        return id;
    }

}
