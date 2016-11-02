package de.triology.cas.services;

import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract class that encapsulates the stages {@link CesServiceManager} operates in.
 * It provides it's registered services via {@link #getRegisteredServices()}.
 *
 * Implementations must initialize their registered services by implementing the template method
 * {@link #initRegisteredServices(Map)}.
 */
abstract class CesServicesManagerStage {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<String> allowedAttributes;

    /**
     * Map to store all registeredServices.
     */
    private final Map<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    CesServicesManagerStage(List<String> allowedAttributes) {
        this.allowedAttributes = allowedAttributes;
    }


    /**
     * @return the services registered in this stage.
     */
    public Map<Long, RegisteredService> getRegisteredServices() {
        if (registeredServices.isEmpty()) {
            initRegisteredServices(registeredServices);
        }
        return this.registeredServices;
    }

    /**
     * Template method that add the stage-specific services to the <code>registeredServices</code>.
     * @param registeredServices
     */
    protected abstract void initRegisteredServices(Map<Long, RegisteredService> registeredServices);


    /**
     * Creates and registers a new service
     *
     * @param name name of the service
     * @param serviceId regex to match requests against
     */
    protected void addNewService(String name, String serviceId) {
        RegexRegisteredService service = new RegexRegisteredService();
        service.setName(name);
        service.setServiceId(serviceId);
        service.setAllowedToProxy(true);
        // TODO Why set this to the initial ID value of the Service? --> Same order for each service!
        service.setEvaluationOrder((int) service.getId());
        service.setAllowedAttributes(allowedAttributes);
        service.setId(EtcdRegistryUtils.findHighestId(registeredServices) + 1);
        registeredServices.put(service.getId(), service);
    }

}
