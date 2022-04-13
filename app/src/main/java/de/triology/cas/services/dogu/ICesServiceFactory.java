package de.triology.cas.services.dogu;

import de.triology.cas.services.CesServiceData;
import org.apereo.cas.services.RegexRegisteredService;

import java.net.URI;

/**
 * Interface for Factories which create Services
 */
public interface ICesServiceFactory {

    /**
     * Creates and registers a new service. Additional attributes can be provided with the serviceData.
     *
     * @param id          id of the service
     * @param fqdn        fqdn of the service
     * @param serviceData data for the service
     */
    RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws CesServiceCreationException;

    static ICesServiceFactory getDefault() {
        return new CesDoguServiceFactory();
    }

}
