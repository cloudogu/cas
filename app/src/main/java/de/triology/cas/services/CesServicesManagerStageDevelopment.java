package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOIDCServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegexRegisteredService;

import java.util.HashMap;
import java.util.Map;

/**
 * Special stage in which a {@link CesServicesManager} operates during development.
 *
 * <p><b>Never use in production</b>, as it accepts <b>all</b> requests from https imaps.</p>
 */
class CesServicesManagerStageDevelopment extends CesServicesManagerStage {

    CesServicesManagerStageDevelopment(CesServiceManagerConfiguration managerConfig) {
        super(managerConfig);
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
        logger.debug("Creating development service...");

        try {
            CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
            Map<String, String> attributes = new HashMap<>();
            attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OIDC_CLIENT_ID, "cas-oidc-client");
            attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OIDC_CLIENT_SECRET_HASH, "df0576c3d0b3b449eef75f71894fffe86baa555eba1d52ed18ec324c96025d10");
            OidcRegisteredService service = (OidcRegisteredService) factory.createNewService(createId(), "", null, new CesServiceData("cas-oidc-client", factory, attributes));
            service.setName("cas-oidc-client");
            service.setServiceId(".*");
            addNewService(service);
            logger.debug("Creating oidc development service... Use the secret: `T0OpxpbdyFixfwMc` and id `cas-oidc-client` for your client.");
        } catch (CesServiceCreationException ignored) {
        }
    }
}
