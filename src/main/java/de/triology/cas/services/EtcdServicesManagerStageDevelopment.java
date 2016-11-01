package de.triology.cas.services;

import org.jasig.cas.services.RegisteredService;

import java.util.List;
import java.util.Map;

/**
 * Special stage in which a {@link EtcdServicesManager} operates during development.
 * <p><b>Never use in production</b>, as it accepts <b>all</b> requests from https imaps.</p>
 */
class EtcdServicesManagerStageDevelopment extends EtcdServicesManagerStage {

    EtcdServicesManagerStageDevelopment(List<String> allowedAttributes) {
        super(allowedAttributes);
    }

    @Override
    protected void initRegisteredServices(Map<Long, RegisteredService> registeredServices) {
        logger.debug("Cas started in development stage. All services can get an ST.");
        addDevService();
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService() {
        // TODO is id=0 necessary for dev services?
        addNewService("10000001", "^(https?|imaps?)://.*");
    }
}
