package de.triology.cas.services.dogu;

import de.triology.cas.services.CesServiceData;
import org.apereo.cas.services.RegexRegisteredService;

import java.net.URI;

public class CesDoguServiceFactory implements ICesServiceFactory {

    public static final String SERVICE_CAS_IDENTIFIER = "cas";

    public RegexRegisteredService createCASService(long id, String fqdn){
        RegexRegisteredService casService = new RegexRegisteredService();
        casService.setId(id);
        casService.setServiceId(String.format("https://%s(:443)?/cas(/.*)?", generateServiceIdFqdnRegex(fqdn)));
        casService.setName(CesDoguServiceFactory.class.getSimpleName() + " " + SERVICE_CAS_IDENTIFIER);
        return casService;
    }

    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutURI, CesServiceData serviceData) throws CesServiceCreationException {
        String fqdnRegex = generateServiceIdFqdnRegex(fqdn);

        RegexRegisteredService service = new RegexRegisteredService();
        service.setId(id);

        String serviceId = String.format("https://%s(:443)?/%s(/.*)?", fqdnRegex, serviceData.getName());
        service.setServiceId(serviceId);
        service.setName(serviceData.getIdentifier());

        if (casLogoutURI != null) {
            String logoutUri = String.format("https://%s/%s%s", fqdn, serviceData.getName(), casLogoutURI);
            service.setLogoutUrl(logoutUri);
        }

        return service;
    }

    public static String generateServiceIdFqdnRegex(String fqdn) {
        if (fqdn == null) {
            return "";
        }

        return "((?i)" + fqdn.replace(".", "\\.") + ")";
    }
}