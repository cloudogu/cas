package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.oidc.services.CesOIDCServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.OidcRegisteredService;

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
        log.debug("Cas started in development stage. All services can get an ST.");
        log.debug("The development stage does not support OAuth services.");
        addDevService();
    }

    @Override
    protected void updateRegisteredServices() {
        log.debug("Cas started in development stage. No services need to be updated");
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService() {
        CasRegisteredService devService = new CasRegisteredService();
        devService.setId(createId());
        devService.setServiceId("^(https?|imaps?)://.*");
        devService.setName("10000001");
        addNewService(devService);
        log.debug("Creating development service...");

        try {
            var factory = new CesOIDCServiceFactory();
            var oidcClientName = "cas-oidc-client";
            Map<String, String> attributes = new HashMap<>();
            attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, oidcClientName);
            attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "df0576c3d0b3b449eef75f71894fffe86baa555eba1d52ed18ec324c96025d10");
            OidcRegisteredService service = (OidcRegisteredService) factory.createNewService(createId(), "", null, new CesServiceData(oidcClientName, factory, attributes));
            service.setName(oidcClientName);
            service.setServiceId(".*");
            addNewService(service);
            log.debug("Creating oidc development service... Use the secret: `T0OpxpbdyFixfwMc` and id `{}` for your client.", oidcClientName);
        } catch (CesServiceCreationException e) {
            log.error("could not start oidc service in development mode: ", e);
        }
    }
}
