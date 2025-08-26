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
        LOGGER.debug("[apply()] Called with service: id={}, name={}, serviceId={}, template={}",
                registeredService.getId(),
                registeredService.getName(),
                registeredService.getServiceId(),
                registeredService.getTemplateName());

        val merged = super.apply(registeredService);

        if (!(merged instanceof CasRegisteredService)) {
            LOGGER.debug("[apply()] Merged service [{}] is NOT of type CasRegisteredService", merged.getClass().getSimpleName());
            return merged;
        }

        val concreteService = (CasRegisteredService) merged;
        val props = concreteService.getProperties();

        LOGGER.debug("[apply()] After merge: id={}, name={}, serviceId={}, template={}, MatchingStrategy={}",
                concreteService.getId(),
                concreteService.getName(),
                concreteService.getServiceId(),
                concreteService.getTemplateName(),
                concreteService.getMatchingStrategy());

        // Dump all properties for debugging
        if (props == null || props.isEmpty()) {
            LOGGER.debug("[apply()] No properties present for service id={}", concreteService.getId());
        } else {
            props.forEach((k, v) -> LOGGER.debug("[apply()] Property {} -> {}", k, v));
        }

        // --- Fallback for 'name' ---
        if ((concreteService.getName() == null || concreteService.getName().isBlank())
                && props.containsKey("ServiceName")) {
            val fallbackName = getFirstProperty(props.get("ServiceName"));
            LOGGER.debug("[apply()] Applying fallback for 'name': {}", fallbackName);
            concreteService.setName(fallbackName);
        } else {
            LOGGER.debug("[apply()] No fallback applied for 'name' (current value={})", concreteService.getName());
        }

        // --- Fallback for 'serviceId' ---
        if ((concreteService.getServiceId() == null || concreteService.getServiceId().isBlank())
                && props.containsKey("Fqdn") && props.containsKey("ServiceName")) {
            val fqdn = getFirstProperty(props.get("Fqdn"));
            val serviceName = getFirstProperty(props.get("ServiceName"));
            val newId = "^https://" + fqdn + "/" + serviceName + "(/.*)?$";
            LOGGER.debug("[apply()] Applying fallback for 'serviceId': {}", newId);
            concreteService.setServiceId(newId);
        } else {
            LOGGER.debug("[apply()] No fallback applied for 'serviceId' (current value={})", concreteService.getServiceId());
        }

        LOGGER.debug("[apply()] Final service state: id={}, name={}, serviceId={}, template={}, MatchingStrategy={}",
                concreteService.getId(),
                concreteService.getName(),
                concreteService.getServiceId(),
                concreteService.getTemplateName(),
                concreteService.getMatchingStrategy());

        return concreteService;
    }


    private String getFirstProperty(final RegisteredServiceProperty prop) {
        return Optional.ofNullable(prop)
                .flatMap(p -> p.getValues().stream().findFirst())
                .orElse("");
    }
}
