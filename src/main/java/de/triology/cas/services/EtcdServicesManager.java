/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import com.github.inspektr.audit.annotation.Audit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.validation.constraints.NotNull;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.jasig.cas.services.ReloadableServicesManager;
import org.jasig.cas.services.ServiceRegistryDao;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Michael Behlendorf
 */
public final class EtcdServicesManager implements ReloadableServicesManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** Instance of ServiceRegistryDao. */
    @NotNull
    private ServiceRegistryDao serviceRegistryDao;

    /** Map to store all services. */
    private ConcurrentHashMap<Long, RegisteredService> services = new ConcurrentHashMap<>();

    private String fqdn;

    private final List<String> allowedAttributes;

    private final JSONParser parser = new JSONParser();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
        public EtcdServicesManager (List<String> allowedAttributes) throws IOException, EtcdException, EtcdAuthenticationException, TimeoutException, ParseException {
        this.allowedAttributes = allowedAttributes;
        services = new ConcurrentHashMap<>();
        EtcdClient etcd = new EtcdClient(URI.create(getEtcdUri()));
        EtcdKeysResponse stageResponse = etcd.get("/config/_global/stage").send().get();

        if (stageResponse.getNode().getValue().equals("development")) {
            logger.debug("cas started in development mode");
            addDevService();

        } else {
            logger.debug("cas started in production mode");
            EtcdResponsePromise<EtcdKeysResponse> response1 = etcd.getDir("/dogu").recursive().send();
            EtcdKeysResponse response2 = etcd.get("/config/_global/fqdn").send().get();
            fqdn = response2.getNode().getValue();
            addServices(response1.get());
            changeLoop(etcd);
        }
    }

    private void changeLoop(EtcdClient etcd) {
        logger.debug("entered changeLoop");
        try {
            EtcdResponsePromise responsePromise = etcd.getDir("/dogu").recursive().waitForChange().send();
            responsePromise.addListener(promise -> {
                try {
                    logger.debug("registered change in /dogu");
                    addServices(etcd.getDir("/dogu").recursive().send().get());                    
                } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException | ParseException ex) {
                    logger.warn("failed to load service", ex);
                } finally {
                    changeLoop(etcd);
                }
            });
        } catch (IOException ex) {
             logger.error("failed to load service", ex);
        }
    }

    private String getEtcdUri() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("/etc/ces/node_master"))) {
            String nodeMaster = reader.readLine();
            if (StringUtils.isBlank(nodeMaster)) {
                throw new IOException("failed to read node_master file");
            }
            return "http://".concat(nodeMaster).concat(":4001");
        }
    }

    public RegisteredService findServiceById(long id) {
        return services.get(id);
    }

    private long findHighestId(ConcurrentHashMap<Long, RegisteredService> map) {
        long id = 0;

        for (Entry<Long,RegisteredService> entry : map.entrySet()) {
            if (entry.getKey()> id) {
                id = entry.getKey();
            }
        }
        return id;
    }

    private JSONObject getCurrentDoguNode(EtcdKeysResponse.EtcdNode doguNode) throws ParseException {
        String version = "";
        JSONObject json = null;
        // get used dogu version
        for (EtcdKeysResponse.EtcdNode leaf : doguNode.getNodes()) {
            if (leaf.getKey().equals(doguNode.getKey() + "/current")) {
                version = leaf.getValue();
            }

        }
        // empty if dogu isnt used
        if (!version.isEmpty()) {
            for (EtcdKeysResponse.EtcdNode leaf : doguNode.getNodes()) {
                if (leaf.getKey().equals(doguNode.getKey() + "/" + version)) {

                    json = (JSONObject) parser.parse(leaf.getValue());
                }
            }
        }

        return json;

    }

    private boolean hasCasDependency(JSONObject json) {
        return json != null && json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas");
    }

    private RegisteredServiceImpl createService(EtcdKeysResponse.EtcdNode doguNode) throws ParseException {
        JSONObject json = getCurrentDoguNode(doguNode);
        // check if dogu needs cas
        if (hasCasDependency(json)) {
            RegisteredServiceImpl service = new RegisteredServiceImpl();
            service.setAllowedToProxy(true);
            service.setName(json.get("Name").toString());
            service.setServiceId("https://" + fqdn + "/" + json.get("Name") + "/");
            service.setEvaluationOrder((int) service.getId());
            service.setAllowedAttributes(allowedAttributes);
            return service;
        }
        return null;
    }

    private void addServices(EtcdKeysResponse response) throws ParseException {
        final ConcurrentHashMap<Long, RegisteredService> localServices =
                new ConcurrentHashMap<>();
        // get all dogu nodes
        for (EtcdKeysResponse.EtcdNode doguNode : response.getNode().getNodes()) {
            RegisteredServiceImpl service = createService(doguNode);
            if (service != null) {
                service.setId(findHighestId(localServices) + 1);
                localServices.put(service.getId(),service);
            }
        }
        try {
            lock.writeLock().lock();
            services = localServices;
        } finally{
            lock.writeLock().unlock();
        }
    }

    private void addDevService() {        
        RegexRegisteredService regexService = new RegexRegisteredService();
        regexService.setServiceId("^(https?|imaps?)://.*");
        regexService.setId(0);
        regexService.setName("10000001");
        regexService.setAllowedToProxy(true);
        regexService.setAllowedAttributes(allowedAttributes);
        services.put(regexService.getId(),regexService);
    }

    @Transactional(readOnly = false)
    @Audit(action = "DELETE_SERVICE", actionResolverName = "DELETE_SERVICE_ACTION_RESOLVER",
            resourceResolverName = "DELETE_SERVICE_RESOURCE_RESOLVER")
    @Override
    public synchronized RegisteredService delete(final long id) {
        final RegisteredService r = findServiceBy(id);
        if (r == null) {
            return null;
        }

        this.serviceRegistryDao.delete(r);
        this.services.remove(id);

        return r;
    }

    /**
     * {@inheritDoc}
     * Note, if the repository is empty, this implementation will return a default service to grant all access.
     * <p>
     * This preserves default CAS behavior.
     */
    @Override
    public RegisteredService findServiceBy(final Service service) {
        final Collection<RegisteredService> c = convertToTreeSet();

        for (final RegisteredService r : c) {
            if (r.matches(service)) {
                return r;
            }
        }

        return null;
    }

    @Override
    public RegisteredService findServiceBy(final long id) {
        final RegisteredService r = this.services.get(id);

        try {
            return r == null ? null : r.clone();
        } catch (final CloneNotSupportedException e) {
            return r;
        }
    }

    protected TreeSet<RegisteredService> convertToTreeSet() {
        return new TreeSet<>(this.services.values());
    }

    @Override
    public Collection<RegisteredService> getAllServices() {
        return Collections.unmodifiableCollection(convertToTreeSet());
    }

    @Override
    public boolean matchesExistingService(final Service service) {
        return findServiceBy(service) != null;
    }

    @Transactional(readOnly = false)
    @Audit(action = "SAVE_SERVICE", actionResolverName = "SAVE_SERVICE_ACTION_RESOLVER",
            resourceResolverName = "SAVE_SERVICE_RESOURCE_RESOLVER")
    @Override
    public synchronized RegisteredService save(final RegisteredService registeredService) {
        final RegisteredService r = this.serviceRegistryDao.save(registeredService);
        this.services.put(r.getId(), r);
        return r;
    }

    @Override
    public void reload() {
        logger.info("Reloading registered services.");
        load();
    }

    private void load() {
        final ConcurrentHashMap<Long, RegisteredService> localServices =
                new ConcurrentHashMap<>();

        for (final RegisteredService r : this.serviceRegistryDao.load()) {
            logger.debug("Adding registered service {}", r.getServiceId());
            localServices.put(r.getId(), r);
        }

        this.services = localServices;
        logger.info(String.format("Loaded %s services.", this.services.size()));
    }
    
}