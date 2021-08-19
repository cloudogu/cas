package de.triology.cas.services;


import org.apereo.cas.services.RegexRegisteredService;

import java.util.List;
import java.util.Map;

/**
 * Special stage in which a {@link CesServicesManager} operates during development.
 *
 * <p><b>Never use in production</b>, as it accepts <b>all</b> requests from https imaps.</p>
 */
class CesServicesManagerStageDevelopment extends CesServicesManagerStage {

    CesServicesManagerStageDevelopment(List<String> allowedAttributes, Map<String, String> attributesMappingRules) {
        super(allowedAttributes, attributesMappingRules);
    }

    @Override
    protected void initRegisteredServices() {
        logger.debug("Cas started in development stage. All services can get an ST.");
        logger.debug("The development stage does not support OAuth services.");
        addDevService();
    }

    @Override
    protected void updateRegisteredServices() {
        logger.debug("Cas started in development stage. No services need to be updated");
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService() {
        RegexRegisteredService devService = new RegexRegisteredService();
        devService.setId(createId());
        devService.setServiceId("^(https?|imaps?)://.*");
        devService.setName("10000001");
        addNewService(devService);
    }
}
