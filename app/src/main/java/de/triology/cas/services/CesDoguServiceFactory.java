package de.triology.cas.services;

import org.jasig.cas.services.RegexRegisteredService;

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
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutURI, CesServiceData serviceData) throws Exception {
        LogoutUriEnabledRegexRegisteredService service = new LogoutUriEnabledRegexRegisteredService();
        service.setId(id);

        String serviceId = "https://" + fqdn + "(:443)?/" + serviceData.getName() + "(/.*)?";
        service.setServiceId(serviceId);
        service.setName(serviceData.getIdentifier());

        if (casLogoutURI != null){
            service.setLogoutUri(casLogoutURI);
        }

        return service;
    }
}