package de.triology.cas.services;

import de.triology.cas.services.attributes.ReturnMappedAttributesPolicy;
import org.apereo.cas.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
abstract class CesServicesManagerStage {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<String> allowedAttributes;
    private final Map<String, String> attributesMappingRules;

    /**
     * Map to store all registeredServices.
     */
    protected final Map<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    CesServicesManagerStage(List<String> allowedAttributes, Map<String, String> attributesMappingRules) {
        this.allowedAttributes = allowedAttributes;
        this.attributesMappingRules = attributesMappingRules;
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
    protected void addNewService(RegexRegisteredService service) {
        service.setProxyPolicy(new RegexMatchingRegisteredServiceProxyPolicy("^https?://.*"));
        service.setEvaluationOrder((int) service.getId());

        service.setAttributeReleasePolicy(new ReturnMappedAttributesPolicy(allowedAttributes, attributesMappingRules));

        ArrayList<String> allowedProviders = new ArrayList<>();
        allowedProviders.add("my-client-name");
        DefaultRegisteredServiceDelegatedAuthenticationPolicy delegatedAuthenticationPolicy = new DefaultRegisteredServiceDelegatedAuthenticationPolicy();
        delegatedAuthenticationPolicy.setPermitUndefined(true);
        delegatedAuthenticationPolicy.setAllowedProviders(allowedProviders);
        DefaultRegisteredServiceAccessStrategy accessStrategy = new DefaultRegisteredServiceAccessStrategy();
        accessStrategy.setDelegatedAuthenticationPolicy(delegatedAuthenticationPolicy);
        service.setAccessStrategy(accessStrategy);

        registeredServices.put(service.getId(), service);
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
