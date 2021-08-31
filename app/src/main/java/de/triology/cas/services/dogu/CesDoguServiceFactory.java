package de.triology.cas.services.dogu;

import de.triology.cas.services.CesServiceData;
import org.apereo.cas.services.RegexRegisteredService;

import java.net.URI;

public class CesDoguServiceFactory implements ICesServiceFactory {

    public static final String SERVICE_CAS_IDENTIFIER = "cas";

    public RegexRegisteredService createCASService(long id, String fqdn){
        RegexRegisteredService casService = new RegexRegisteredService();
        casService.setId(id);
        casService.setServiceId("https://" + fqdn + "/cas/.*");
        casService.setName(CesDoguServiceFactory.class.getSimpleName() + " " + SERVICE_CAS_IDENTIFIER);
        return casService;
    }

    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutURI, CesServiceData serviceData) throws CesServiceCreationException {
        RegexRegisteredService service = new RegexRegisteredService();
        service.setId(id);

        String serviceId = "https://" + fqdn + "(:443)?/" + serviceData.getName() + "(/.*)?";
        service.setServiceId(serviceId);
        service.setName(serviceData.getIdentifier());

        if (casLogoutURI != null) {
            String logoutUri = "https://" + fqdn + "/" + serviceData.getName() + casLogoutURI;
            service.setLogoutUrl(logoutUri);
        }

        return service;
    }
}