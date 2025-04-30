package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServicesTemplatesManager;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.util.serialization.StringSerializer;

import java.io.File;
import java.util.*;

@Slf4j
public class CesLegacyCompatibleTemplatesManager extends DefaultRegisteredServicesTemplatesManager {

    public CesLegacyCompatibleTemplatesManager(Collection<File> templateDefinitionFiles,
                                               StringSerializer<RegisteredService> registeredServiceSerializer) {
        super(templateDefinitionFiles, registeredServiceSerializer);
    }

    @Override
    public RegisteredService apply(final RegisteredService registeredService) {
        val merged = super.apply(registeredService);

        if (!(merged instanceof CasRegisteredService)) {
            LOGGER.debug("Merged service [{}] is not of type CasRegisteredService", merged);
            return merged;
        }

        val concreteService = (CasRegisteredService) merged;
        val props = concreteService.getProperties();

        // Fallback for 'name'
        if ((concreteService.getName() == null || concreteService.getName().isBlank())
                && props.containsKey("ServiceName")) {
            val fallbackName = getFirstProperty(props.get("ServiceName"));
            concreteService.setName(fallbackName);
        }

        // Fallback for 'serviceId'
        if ((concreteService.getServiceId() == null || concreteService.getServiceId().isBlank())
                && props.containsKey("Fqdn") && props.containsKey("ServiceName")) {
            val fqdn = getFirstProperty(props.get("Fqdn"));
            val serviceName = getFirstProperty(props.get("ServiceName"));
            concreteService.setServiceId("^https://" + fqdn + "/" + serviceName + "(/.*)?$");
        }

        LOGGER.debug("Applying template to service: name={}, serviceId={}, template={}, MatchingStrategy={}\"", 
        registeredService.getName(),
        registeredService.getServiceId(),
        registeredService.getTemplateName(),
        registeredService.getMatchingStrategy()
        );
        return concreteService;
    }

    private String getFirstProperty(final RegisteredServiceProperty prop) {
        return Optional.ofNullable(prop)
                .flatMap(p -> p.getValues().stream().findFirst())
                .orElse("");
    }
}
