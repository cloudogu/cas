/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ReloadableServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
// TODO rename to CasServiceManager or CesCasServiceManager? --> No direct connection to etcd!
public class EtcdServicesManager implements ReloadableServicesManager {

    /**
     * This triggers operation in development stage.
     */
    static final String STAGE_DEVELOPMENT = "development";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private EtcdServicesManagerStage serviceStage;

    public EtcdServicesManager(List<String> allowedAttributes, String stage, Registry registry) {
        serviceStage = createStage(stage, allowedAttributes, registry);
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        return Collections.unmodifiableCollection(serviceStage.getRegisteredServices().values());
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
    public RegisteredService findServiceBy(final long id) {
        final RegisteredService r = serviceStage.getRegisteredServices().get(id);

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
     * @return a new instance of the {@link EtcdServicesManagerStage}, depending on the <code>stageString</code> parameter.
     */
    protected EtcdServicesManagerStage createStage(String stageString, List<String> allowedAttributes, Registry registry) {
        if (!STAGE_DEVELOPMENT.equals(stageString)) {
            return new EtcdServicesManagerStageProductive(allowedAttributes, registry);
        } else {
            return new EtcdServicesManagerStageDevelopment(allowedAttributes);
        }
    }
}
