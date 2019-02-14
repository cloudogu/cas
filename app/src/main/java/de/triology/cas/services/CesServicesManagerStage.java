package de.triology.cas.services;

import org.jasig.cas.services.RegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An abstract class that encapsulates the stages {@link CesServiceManager} operates in.
 * It provides it's registered services via {@link #getRegisteredServices()}.
 * <p>
 * Implementations must initialize their registered services by implementing the template method
 * {@link #initRegisteredServices(Map)}.
 */
abstract class CesServicesManagerStage {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<String> allowedAttributes;

    /**
     * Map to store all registeredServices.
     */
    protected final Map<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    CesServicesManagerStage(List<String> allowedAttributes) {
        this.allowedAttributes = allowedAttributes;
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
     * Creates and registers a new service with a specific logout URI
     *
     * @param name      name of the service
     * @param serviceId regex to match requests against
     * @param logoutUri service specific logout URI
     */
    protected void addNewService(String name, String serviceId, URI logoutUri) {
        LogoutUriEnabledRegexRegisteredService service = new LogoutUriEnabledRegexRegisteredService();
        service.setName(name);
        service.setServiceId(serviceId);
        service.setAllowedToProxy(true);
        // TODO Why set this to the initial ID value of the Service? --> Same order for each service!
        service.setEvaluationOrder((int) service.getId());
        service.setAllowedAttributes(allowedAttributes);
        service.setId(createId());
        if (logoutUri != null){
            service.setLogoutUri(logoutUri);
        }
        registeredServices.put(service.getId(), service);
    }

    /**
     * Creates and registers a new service
     *
     * @param name      name of the service
     * @param serviceId regex to match requests against
     */
    protected void addNewService(String name, String serviceId) {
        addNewService(name, serviceId, null);
    }

    /**
     * @return a new numeric ID for a registered service
     */
    private long createId() {
        // TODO Wouldn't it be simpler an less error prone to use an AtomicLong instead?
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