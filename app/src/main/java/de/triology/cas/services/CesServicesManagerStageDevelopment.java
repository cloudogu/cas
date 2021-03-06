package de.triology.cas.services;


import org.jasig.cas.services.RegexRegisteredService;

import java.util.List;

/**
 * Special stage in which a {@link CesServicesManager} operates during development.
 *
 * <p><b>Never use in production</b>, as it accepts <b>all</b> requests from https imaps.</p>
 */
class CesServicesManagerStageDevelopment extends CesServicesManagerStage {

    CesServicesManagerStageDevelopment(List<String> allowedAttributes) {
        super(allowedAttributes);
    }

    @Override
    protected void initRegisteredServices() {
        logger.debug("Cas started in development stage. All services can get an ST.");
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
