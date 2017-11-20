package de.triology.cas.services;

import org.jasig.cas.services.RegisteredService;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The stage in which a {@link CesServiceManager} operates in production.
 * Services accesible via CAS ({@link RegisteredService}s) are queried from a {@link Registry}.
 * For each Dogu that is accessible via CAS, one {@link RegisteredService} is returned. An additional service allows
 * CAS to access itself.
 */
class CesServicesManagerStageProductive extends CesServicesManagerStage {

    /**
     * Name of the special service that allows cas to access itself. See {@link #addCasService()}.
     */
    private static final String SERVICE_NAME_CAS = "cas";

    private String fqdn;

    private Registry registry;

    private boolean initialized = false;

    CesServicesManagerStageProductive(List<String> allowedAttributes, Registry registry) {
        super(allowedAttributes);
        this.registry = registry;
    }

    @Override
    protected synchronized void initRegisteredServices(Map<Long, RegisteredService> registeredServices) {
        if(initialized) {
            logger.info("Already initialized CesServicesManager. Doing nothing.");
            return;
        }
        try {
            logger.debug("Cas started in production stage. Only installed dogus can get an ST.");
            fqdn = registry.getFqdn();
            synchronizeServicesWithRegistry(registeredServices);
            addCasService();
            registerChangeListener(registeredServices);
            initialized = true;
        } catch (RegistryException ex) {
            logger.warn("failed to get data from registry", ex);
        }
    }

    @Override
    protected void updateRegisteredServices(Map<Long, RegisteredService> registeredServices) {
        synchronizeServicesWithRegistry(registeredServices);
    }

    /**
     * Synchronize services from {@link #registry} to <code>existingServices</code>.
     * That is, remove the ones that are not present in{@link #registry} and add the ones that are only present
     * in {@link #registry} to <code>existingServices</code>.
     */
    private void synchronizeServicesWithRegistry(Map<Long, RegisteredService> existingServices) {
        try {
            synchronizeServices(registry.getDogus(), existingServices);
        } catch (RegistryException ex) {
            logger.warn("failed to update servicesManager", ex);
        }
        logger.info(String.format("Loaded %s services.", getRegisteredServices().size()));
    }

    /**
     * Detects when a new dogu is installed or an existing one is removed
     * @param registeredServices
     */
    private void registerChangeListener(Map<Long, RegisteredService> registeredServices) {
        logger.debug("entered registerChangeListener");
        try {
            registry.addDoguChangeListener(()-> {
                logger.debug("registered change in /dogu");
                synchronizeServicesWithRegistry(registeredServices);
            });
        } catch (RegistryException ex) {
            logger.error("failed to synchronizeServicesWithRegistry service", ex);
        }
    }

    /**
     * Creates and registers a new service for an given name
     */
    private void addNewService(String name) {
        addNewService(name, "https://" + fqdn + "(:443)?/" + name + "(/.*)?");
    }

    /**
     * Synchronize services from <code>newServices</code> to <code>existingServices</code>.
     * That is, remove the ones that are not present in <code>newServices</code> and add the ones that are only present
     * in <code>newServices</code> to <code>existingServices</code>.
     */
    private void synchronizeServices(List<String> newServiceNames, Map<Long, RegisteredService> existingServices) {
        removeServicesThatNoLongerExist(newServiceNames, existingServices);
        addServicesThatDoNotExistYet(newServiceNames, existingServices);
    }

    /**
     * First operation of {@link #synchronizeServices(List, Map)}: Remove Services that are not present in
     * <code>newServices</code> from <code>existingServices</code>.
     */
    private void removeServicesThatNoLongerExist(List<String> newServiceNames,
                                                 Map<Long, RegisteredService> existingServices) {
        for (Iterator<Map.Entry<Long, RegisteredService>> existingServiceIterator =
             existingServices.entrySet().iterator(); existingServiceIterator.hasNext(); ) {
            Map.Entry<Long, RegisteredService> existingServiceEntry = existingServiceIterator.next();

            if (!newServiceNames.contains(existingServiceEntry.getValue().getName()) &&
                // Special case: Cas service is not added via registry. Don't delete it!
                !SERVICE_NAME_CAS.equals(existingServiceEntry.getValue().getName())) {
                existingServiceIterator.remove();
            }
        }
    }

    /**
     * Second operation of {@link #synchronizeServices(List, Map)}: Add services that are only present in
     * <code>newServices</code> to <code>existingServices</code>.
     */
    private void addServicesThatDoNotExistYet(List<String> newServiceNames,
                                              Map<Long, RegisteredService> existingServices) {
        Set<String> existingServiceNames =
                existingServices.values().stream().map(RegisteredService::getName).collect(Collectors.toSet());

        newServiceNames.stream()
                       .filter(newServiceName -> !existingServiceNames.contains(newServiceName))
                       .forEach(this::addNewService);
    }

    /**
     * This is necessary since cas needs a ST in clearPass workflow
     */
    private void addCasService() {
        addNewService(SERVICE_NAME_CAS, "https://" + fqdn + "/cas/.*");
    }

}
