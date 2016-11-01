/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import org.apache.commons.lang.StringUtils;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ReloadableServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adds for each dogu, which needs cas, a service to registeredServices. These
 * dogus getting identified with etcd: Installed dogus have a directory
 * '/dogu/${name of dogu}/current' with their used version. Further 'cas' has to
 * be in the dependencies of the dogu. Changes of the '/dogu' directory will be
 * noticed and registeredServices updated. Every service will be accepted if the
 * ecosystem is in development stage.
 *
 * @author Michael Behlendorf
 */
public final class EtcdServicesManager implements ReloadableServicesManager {

    /**
     * Name of the special service that allows cas to access itself. See {@link #addCasService(ConcurrentHashMap)}.
     */
    private static final String SERVICE_NAME_CAS = "cas";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Map to store all registeredServices.
     */
    private ConcurrentHashMap<Long, RegisteredService> registeredServices = new ConcurrentHashMap<>();

    private String fqdn;

    private String stage;

    private final List<String> allowedAttributes;

    private Registry registry;

    public EtcdServicesManager(List<String> allowedAttributes, String stage, Registry registry) {
        this.allowedAttributes = allowedAttributes;
        this.stage = stage;
        this.registry = registry;

        // TODO Single responsibility: Extract separate classes for Production and Development modes
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        return Collections.unmodifiableCollection(getRegisteredServices().values());
    }

    @Override
    public RegisteredService findServiceBy(final Service service) {
        final Collection<RegisteredService> registeredServices = getRegisteredServices().values();

        for (final RegisteredService registeredService : registeredServices) {
            if (registeredService.matches(service)) {
                return registeredService;
            }
        }

        return null;
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        final RegisteredService r = getRegisteredServices().get(id);

        try {
            return r == null ? null : r.clone();
        } catch (final CloneNotSupportedException e) {
            return r;
        }
    }

    @Override
    public boolean matchesExistingService(final Service service) {
        return findServiceBy(service) != null;
    }

    @Override
    public void reload() {
        logger.info("Cas wants to reload registered services.");
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService delete(final long id) {
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    /**
     * Synchronize services from {@link #registry} to <code>existingServices</code>.
     * That is, remove the ones that are not present in{@link #registry} and add the ones that are only present
     * in {@link #registry} to <code>existingServices</code>.
     */
    private void synchronizeServicesWithRegistry(ConcurrentHashMap<Long, RegisteredService> existingServices) {
        try {
            synchronizeServices(registry.getDogus(), existingServices);
        } catch (RegistryException ex) {
            logger.warn("failed to update servicesManager", ex);
        }
        logger.info(String.format("Loaded %s services.", getRegisteredServices().size()));
    }

    private ConcurrentHashMap<Long, RegisteredService> getRegisteredServices() {
        if (this.registeredServices.isEmpty()) {
            initRegisteredServices();
        }
        return this.registeredServices;
    }

    private void initRegisteredServices() {
        // depends on stage configured in cas.properties
        if (!"development".equals(stage)) {
            initProductionMode();
        } else {
            initDevelopmentMode();
        }
    }

    private void initProductionMode() {
        try {
            logger.debug("Cas started in production stage. Only installed dogus can get an ST.");
            fqdn = registry.getFqdn();
            synchronizeServicesWithRegistry(registeredServices);
            addCasService(registeredServices);
            registerChangeListener();
        } catch (RegistryException ex) {
            logger.warn("failed to get data from registry", ex);
        }
    }

    private void initDevelopmentMode() {
        logger.debug("Cas started in development stage. All services can get an ST.");
        addDevService(registeredServices);
    }

    /**
     * Detects when a new dogu is installed or an existing one is removed
     */
    private void registerChangeListener() {
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
     * Creates a RegexRegisteredService for an given name
     */
    private RegexRegisteredService createService(String name, Map<Long, RegisteredService> existingServices) {
        RegexRegisteredService service = new RegexRegisteredService();
        String[] nameArray = StringUtils.split(name, "/");
        service.setAllowedToProxy(true);
        service.setName(name);
        service.setServiceId("https://" + fqdn + "(:443)?/" + nameArray[nameArray.length - 1] + "(/.*)?");
        // TODO Why set this to the initial ID value of the Service? --> Same order for each service!
        service.setEvaluationOrder((int) service.getId());
        service.setAllowedAttributes(allowedAttributes);
        service.setId(EtcdRegistryUtils.findHighestId(existingServices) + 1);
        return service;
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

            if (newServiceNames.contains(existingServiceEntry.getValue().getName()) &&
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
                       .forEach(newServiceName -> addNewService(newServiceName, existingServices));
    }

    /**
     * Creates a new service and adds it to the <code>existingServices</code>.
     */
    private void addNewService(String newServiceName, Map<Long, RegisteredService> existingServices) {
        RegexRegisteredService newService = createService(newServiceName, existingServices);
        existingServices.put(newService.getId(), newService);
    }

    /**
     * This is necessary since cas needs a ST in clearPass workflow
     */
    private void addCasService(ConcurrentHashMap<Long, RegisteredService> existingServices) {
        RegexRegisteredService service = createService(SERVICE_NAME_CAS, existingServices);
        service.setServiceId("https://" + fqdn + "/cas/.*");
        existingServices.put(service.getId(), service);
    }

    /**
     * The dev service accepts all services
     */
    private void addDevService(ConcurrentHashMap<Long, RegisteredService> existingServices) {
        RegexRegisteredService service = new RegexRegisteredService();
        service.setServiceId("^(https?|imaps?)://.*");
        service.setId(0);
        service.setName("10000001");
        service.setAllowedToProxy(true);
        service.setAllowedAttributes(allowedAttributes);
        existingServices.put(service.getId(), service);
    }
}
