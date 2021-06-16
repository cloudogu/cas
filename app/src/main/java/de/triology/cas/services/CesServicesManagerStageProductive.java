package de.triology.cas.services;

import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.oauth.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The stage in which a {@link CesServicesManager} operates in production.
 * Services accesible via CAS ({@link RegisteredService}s) are queried from a {@link Registry}.
 * For each Dogu that is accessible via CAS, one {@link RegisteredService} is returned. An additional service allows
 * CAS to access itself.
 * For each OAuth client that is accessuble via CAS, one {@link RegisteredService} is returned. An additional service
 * for the OAuth callback is also created.
 */
class CesServicesManagerStageProductive extends CesServicesManagerStage {

    private String fqdn;

    private Registry registry;

    private List<CesServiceData> persistentServices;

    private final CesOAuthServiceFactory oAuthServiceFactory;
    private final CesDoguServiceFactory doguServiceFactory;

    private boolean initialized = false;

    CesServicesManagerStageProductive(List<String> allowedAttributes, Registry registry) {
        super(allowedAttributes);
        this.registry = registry;
        this.persistentServices = new ArrayList<>();
        this.doguServiceFactory = new CesDoguServiceFactory();
        this.oAuthServiceFactory = new CesOAuthServiceFactory();
    }

    /**
     * Initialize the registered services found in registry.
     * This is synchronized because otherwise two parallel calls could lead
     * to multiple initializations and an inconsistent state (e.g. cas-service multiple times).
     * Parallel calls can happen since we call {@code initRegisteredServices()} in {@link #getRegisteredServices()}.
     * This will not be an performance issue because this method is only called once, after startup.
     */
    @Override
    protected synchronized void initRegisteredServices() {
        if (isInitialized()) {
            logger.info("Already initialized CesServicesManager. Doing nothing.");
            return;
        }
        logger.debug("Cas started in production stage. Only installed dogus can get an ST.");
        fqdn = registry.getFqdn();
        addPersistentServices();
        synchronizeServicesWithRegistry();
        registerChangeListener();
        initialized = true;
        logger.debug("Finished initialization of registered services");
    }

    private boolean isInitialized() {
        return initialized;
    }

    @Override
    protected void updateRegisteredServices() {
        if (isInitialized()) {
            synchronizeServicesWithRegistry();
        } else {
            initRegisteredServices();
        }
    }

    /**
     * Synchronize services from {@link #registry} to <code>registeredServices</code>.
     * That is, remove the ones that are not present in{@link #registry} and add the ones that are only present
     * in {@link #registry} to <code>registeredServices</code>.
     */
    private void synchronizeServicesWithRegistry() {
        logger.debug("Synchronize services with registry");
        List<CesServiceData> newServices = new ArrayList<>(persistentServices);
        newServices.addAll(registry.getInstalledOAuthCASServiceAccounts(oAuthServiceFactory));
        newServices.addAll(registry.getInstalledDogusWhichAreUsingCAS(doguServiceFactory));
        synchronizeServices(newServices);
        logger.info("Loaded {} services!", registeredServices.size());
    }

    /**
     * Detects when a new dogu is installed or an existing one is removed
     */
    private void registerChangeListener() {
        logger.debug("Entered registerChangeListener");
        registry.addDoguChangeListener(() -> {
            logger.debug("Registered change in /dogu");
            synchronizeServicesWithRegistry();
        });
    }

    /**
     * Creates and registers a new service for an given name
     */
    void addNewService(CesServiceData serviceData) {
        String serviceName = serviceData.getName();
        logger.debug("Add new service: {}", serviceName);
        try {
            addNewService(serviceName, serviceData);
        } catch (CesServiceCreationException e) {
            logger.error("Failed to create service [{}]. Skip service creation - {}", serviceName, e.toString());
        }
    }

    /**
     * Creates and registers a new service for an given name
     */
    void addNewService(String serviceName, CesServiceData serviceData) throws CesServiceCreationException {
        try {
            URI logoutUri = registry.getCasLogoutUri(serviceName);
            RegexRegisteredService service = serviceData.getFactory().createNewService(createId(), fqdn, logoutUri, serviceData);
            addNewService(service);
        } catch (GetCasLogoutUriException e) {
            logger.debug("GetCasLogoutUriException: CAS logout URI of service {} could not be retrieved: {}", serviceName, e.toString());
            logger.info("Adding service without CAS logout URI");
            RegexRegisteredService service = serviceData.getFactory().createNewService(createId(), fqdn, null, serviceData);
            addNewService(service);
        }
    }

    /**
     * Synchronize services from <code>newServices</code> to <code>registeredServices</code>.
     * That is, remove the ones that are not present in <code>newServices</code> and add the ones that are only present
     * in <code>newServices</code> to <code>registeredServices</code>.
     */
    private void synchronizeServices(List<CesServiceData> newServiceNames) {
        removeServicesThatNoLongerExist(newServiceNames);
        addServicesThatDoNotExistYet(newServiceNames);
    }

    /**
     * First operation of {@link #synchronizeServices(List)}: Remove Services that are not present in
     * <code>newServices</code> from <code>registeredServices</code>.
     */
    private void removeServicesThatNoLongerExist(List<CesServiceData> newServiceNames) {
        List<String> newServicesIdentifiers = newServiceNames.stream()
                .map(CesServiceData::getIdentifier)
                .collect(Collectors.toList());

        List<String> toBeRemovedServices = registeredServices.values()
                .stream().map(RegisteredService::getName)
                .filter(serviceName -> !newServicesIdentifiers.contains(serviceName))
                .collect(Collectors.toList());

        registeredServices.values().stream()
                .filter(service -> toBeRemovedServices.contains(service.getName()))
                .forEach(service -> registeredServices.remove(service.getId()));
    }

    /**
     * Second operation of {@link #synchronizeServices(List)}: Add services that are only present in
     * <code>newServices</code> to <code>registeredServices</code>.
     */
    private void addServicesThatDoNotExistYet(List<CesServiceData> newServiceNames) {
        Set<String> existingServiceNames =
                registeredServices.values().stream().map(RegisteredService::getName).collect(Collectors.toSet());

        Set<String> newServices = newServiceNames.stream()
                .map(CesServiceData::getIdentifier)
                .filter(serviceName -> !existingServiceNames.contains(serviceName))
                .collect(Collectors.toSet());

        newServiceNames.stream().filter(service -> newServices.contains(service.getIdentifier()))
                .forEach(this::addNewService);
    }

    /**
     * persistent services will not be removed
     */
    public void addPersistentServices() {
        //This is necessary since cas needs a Service Ticket in clearPass workflow
        logger.info("Creating cas service for clearPass workflow");
        addNewService(doguServiceFactory.createCASService(createId(), fqdn));
        persistentServices.add(new CesServiceData(CesDoguServiceFactory.SERVICE_CAS_IDENTIFIER, doguServiceFactory));
    }
}
