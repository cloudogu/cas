package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.oidc.services.CesOIDCServiceFactory;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
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

    public final CesOAuthServiceFactory oAuthServiceFactory;
    public final CesOIDCServiceFactory oidcServiceFactory;
    public final CesDoguServiceFactory doguServiceFactory;

    private boolean initialized = false;

    CesServicesManagerStageProductive(CesServiceManagerConfiguration managerConfig, Registry registry) {
        super(managerConfig);
        this.registry = registry;
        this.persistentServices = new ArrayList<>();
        this.doguServiceFactory = new CesDoguServiceFactory();
        this.oAuthServiceFactory = new CesOAuthServiceFactory();
        this.oidcServiceFactory = new CesOIDCServiceFactory();
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
            LOG.info("Already initialized CesServicesManager. Doing nothing.");
            return;
        }
        LOG.debug("Cas started in production stage. Only installed dogus can get an ST.");
        fqdn = registry.getFqdn();
        addPersistentServices();
        synchronizeServicesWithRegistry();
        registerChangeListener();
        initialized = true;
        LOG.debug("Finished initialization of registered services");
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
        LOG.debug("Synchronize services with registry");
        List<CesServiceData> newServices = new ArrayList<>(persistentServices);
        newServices.addAll(registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OAUTH, oAuthServiceFactory));
        newServices.addAll(registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OIDC, oidcServiceFactory));
        List<String> serviceAccountServices = newServices.stream().map(data -> data.getName()).collect(Collectors.toList());

        List<CesServiceData> doguServices = registry.getInstalledDogusWhichAreUsingCAS(doguServiceFactory);
        newServices.addAll(doguServices.stream().filter(service -> !serviceAccountServices.contains(service.getName())).collect(Collectors.toList()));
        synchronizeServices(newServices);
        LOG.info("Loaded {} services:", registeredServices.size());
        registeredServices.values().forEach(e -> LOG.debug("[{}]", e));
    }

    /**
     * Detects when a new dogu is installed or an existing one is removed
     */
    private void registerChangeListener() {
        LOG.debug("Entered registerChangeListener");
        registry.addDoguChangeListener(() -> {
            LOG.debug("Registered change in /dogu");
            synchronizeServicesWithRegistry();
        });
    }

    /**
     * Creates and registers a new service for an given name
     */
    void addNewService(CesServiceData serviceData) {
        String serviceName = serviceData.getName();
        LOG.debug("Add new service: {}", serviceName);
        try {
            addNewService(serviceName, serviceData);
        } catch (CesServiceCreationException e) {
            LOG.error("Failed to create service [{}]. Skip service creation - {}", serviceName, e.toString());
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
            LOG.debug("GetCasLogoutUriException: CAS logout URI of service {} could not be retrieved: {}", serviceName, e.toString());
            LOG.info("Adding service without CAS logout URI");
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
        //This is necessary for the oauth workflow
        LOG.info("Creating cas service for oauth/oidc workflow");
        addNewService(doguServiceFactory.createCASService(createId(), fqdn));
        persistentServices.add(new CesServiceData(CesDoguServiceFactory.SERVICE_CAS_IDENTIFIER, doguServiceFactory));
    }
}
