/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

/**
 * Manages the Dogus that are accessible via CAS within the Cloudogu Ecosystem.
 * Depending on the {@link CesServicesManagerStage} ({@link CesServicesManagerStageDevelopment} or
 * {@link CesServicesManagerStageProductive}), a number of {@link RegisteredService}s is returned.
 */
public class CesServicesManager implements ServicesManager {

    /**
     * This triggers operation in development stage.
     */
    static final String STAGE_DEVELOPMENT = "development";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CesServicesManagerStage serviceStage;

    public CesServicesManager(CesServiceManagerConfiguration managerConfig, Registry registry) {
        serviceStage = createStage(managerConfig, registry);
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        logger.debug("Entered getAllServices method");
        return Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values());
    }

    @Override
    public Collection<RegisteredService> load() {
        logger.info("Cas wants to reload registered services.");
        serviceStage.updateRegisteredServices();
        return serviceStage.getRegisteredServices().values();
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
