package de.triology.cas.services;

import org.jasig.cas.services.RegexRegisteredService;

import java.net.URI;

/**
 * Interface for Factories which create Services
 */
public interface ICesServiceFactory {

    /**
     * Creates and registers a new service. Additional attributes can be provided with the serviceData.
     *
     * @param id      id of the service
     * @param fqdn      fqdn of the service
     * @param serviceData      data for the service
     */
    RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws Exception;

    static ICesServiceFactory getDefault() {
        return new CesDoguServiceFactory();
    }

}
