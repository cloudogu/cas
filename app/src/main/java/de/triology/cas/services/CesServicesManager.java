package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the Dogus that are accessible via CAS within the Cloudogu Ecosystem.
 * Depending on the {@link CesServicesManagerStage} ({@link CesServicesManagerStageDevelopment} or
 * {@link CesServicesManagerStageProductive}), a number of {@link RegisteredService}s is returned.
 */
@Slf4j
public class CesServicesManager implements ServicesManager {

    /**
     * This triggers operation in development stage.
     */
    static final String STAGE_DEVELOPMENT = "development";

    private final CesServicesManagerStage serviceStage;

    public CesServicesManager(CesServiceManagerConfiguration managerConfig, Registry registry) {
        serviceStage = createStage(managerConfig, registry);
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        LOGGER.trace("Entered getAllServices method with return {}", Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values()));
        return Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values());
    }

    @Override
    public Collection<RegisteredService> getAllServicesOfType(final Class clazz) {
        LOGGER.trace("Entered getAllServicesOfType method with type: {}", clazz);
        if (supports(clazz)) {
            return serviceStage.getRegisteredServices().values()
                    .stream()
                    .filter(s -> clazz.isAssignableFrom(s.getClass()))
                    .sorted()
                    .peek(RegisteredService::initialize)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<RegisteredService> load() {
        LOGGER.info("Cas wants to reload registered services.");
        serviceStage.updateRegisteredServices();
        return serviceStage.getRegisteredServices().values();
    }

    @Override
    public Collection<RegisteredService> getServicesForDomain(String domain) {
        LOGGER.trace("getServicesForDomain: {}", domain);
        throw new UnsupportedOperationException("Operation getServicesForDomain is not supported.");
    }

    @Override
    public RegisteredService findServiceBy(final Service service) {
        LOGGER.trace("findServiceBy: {}", service);
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
        LOGGER.trace("findServiceBy1: {}", clazz);
        throw new UnsupportedOperationException("Operation findServiceBy is not supported.");
    }

    @Override
    public RegisteredService findServiceBy(final Service requestedService, final Class clazz) {
        if (requestedService == null) {
            return null;
        }
        RegisteredService service = findServiceBy(requestedService);
        if (service != null && clazz.isAssignableFrom(service.getClass())) {
            return service;
        }
        return null;
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        LOGGER.trace("findServiceBy: {}", id);
        return serviceStage.getRegisteredServices().get(id);
    }

    @Override
    public RegisteredService findServiceByName(String name) {
        LOGGER.trace("findServiceByName: {}", name);
        throw new UnsupportedOperationException("Operation findServiceByName is not supported.");
    }

    @Override
    public void save(Stream<RegisteredService> toSave) {
        LOGGER.trace("save1: {}", toSave);
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        LOGGER.trace("save2: {}", registeredService);
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public RegisteredService save(RegisteredService registeredService, boolean publishEvent) {
        LOGGER.trace("save3: {} - {}", registeredService, publishEvent);
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public void save(Supplier<RegisteredService> supplier, Consumer<RegisteredService> andThenConsume, long countExclusive) {
        LOGGER.trace("save4: {} - {}", supplier, andThenConsume);
        throw new UnsupportedOperationException("Operation save is not supported.");
    }

    @Override
    public void deleteAll() {
        LOGGER.trace("deleteAll:");
        throw new UnsupportedOperationException("Operation deleteAll is not supported.");
    }

    @Override
    public RegisteredService delete(final long id) {
        LOGGER.trace("delete1: {}", id);
        throw new UnsupportedOperationException("Operation delete is not supported.");
    }

    @Override
    public RegisteredService delete(RegisteredService svc) {
        LOGGER.trace("delete2: {}", svc);
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
