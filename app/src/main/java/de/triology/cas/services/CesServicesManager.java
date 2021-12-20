/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import lombok.val;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the Dogus that are accessible via CAS within the Cloudogu Ecosystem.
 * Depending on the {@link CesServicesManagerStage} ({@link CesServicesManagerStageDevelopment} or
 * {@link CesServicesManagerStageProductive}), a number of {@link RegisteredService}s is returned.
 */
public class CesServicesManager implements ServicesManager {
    private static final Logger LOG = LoggerFactory.getLogger(CesServicesManager.class);

    /**
     * This triggers operation in development stage.
     */
    static final String STAGE_DEVELOPMENT = "development";
    
    private static Predicate<RegisteredService> getRegisteredServicesFilteringPredicate(
            final Predicate<RegisteredService>... p) {
        val predicates = Stream.of(p).collect(Collectors.toCollection(ArrayList::new));
        return predicates.stream().reduce(x -> true, Predicate::and);
    }
    
    private CesServicesManagerStage serviceStage;

    public CesServicesManager(CesServiceManagerConfiguration managerConfig, Registry registry) {
        serviceStage = createStage(managerConfig, registry);
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        LOG.debug("Entered getAllServices method");
        return Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values());
    }

    @Override
    public <T extends RegisteredService> Collection<T> getAllServicesOfType(Class<T> clazz) {
        if (supports(clazz)) {
            Collection<RegisteredService> services = serviceStage.getRegisteredServices().values()
                    .stream()
                    .filter(s -> clazz.isAssignableFrom(s.getClass()))
                    .filter(getRegisteredServicesFilteringPredicate())
                    .sorted()
                    .peek(RegisteredService::initialize)
                    .collect(Collectors.toList());
            return (Collection<T>) services;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<RegisteredService> load() {
        LOG.info("Cas wants to reload registered services.");
        serviceStage.updateRegisteredServices();
        return serviceStage.getRegisteredServices().values();
    }

    @Override
    public Collection<RegisteredService> getServicesForDomain(String domain) {
        throw new UnsupportedOperationException("Operation getServicesForDomain is not supported.");
    }

    @Override
    public RegisteredService findServiceBy(final Service service) {
        final Collection<RegisteredService> registeredServices = serviceStage.getRegisteredServices().values();

        for (final RegisteredService registeredService : registeredServices) {
            if (registeredService.matches(service)) {
                return registeredService;
            }
        }

        return null;
    }

    @Override
    public Collection<RegisteredService> findServiceBy(Predicate<RegisteredService> clazz) {
        throw new UnsupportedOperationException("Operation findServiceBy is not supported.");
    }

    @Override
    public <T extends RegisteredService> T findServiceBy(Service serviceId, Class<T> clazz) {
        throw new UnsupportedOperationException("Operation findServiceBy is not supported.");
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        return serviceStage.getRegisteredServices().get(id);
    }

    @Override
    public RegisteredService findServiceByName(String name) {
        throw new UnsupportedOperationException("Operation findServiceByName is not supported.");
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService save(RegisteredService registeredService, boolean publishEvent) {
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public void deleteAll() {
        throw new UnsupportedOperationException("Operation deleteAll is not supported.");
    }

    @Override
    public RegisteredService delete(final long id) {
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    @Override
    public RegisteredService delete(RegisteredService svc) {
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    /**
     * @return a new instance of the {@link CesServicesManagerStage}, depending on the <code>stageString</code> parameter.
     */
    protected CesServicesManagerStage createStage(CesServiceManagerConfiguration managerConfig, Registry registry) {
        if (!STAGE_DEVELOPMENT.equals(managerConfig.getStage())) {
            return new CesServicesManagerStageProductive(managerConfig, registry);
        } else {
            return new CesServicesManagerStageDevelopment(managerConfig);
        }
    }
}
