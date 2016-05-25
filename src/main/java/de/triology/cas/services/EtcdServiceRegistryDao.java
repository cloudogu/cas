/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;
import org.apache.commons.lang.StringUtils;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.jasig.cas.services.ServiceRegistryDao;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Adds for each dogu, which needs cas, a service to registeredServices.
 *  These dogus getting identified with etcd:
 *      Installed dogus have a directory '/dogu/${name of dogu}/current' with their used version.
 *      Further 'cas' has to be in the dependencies of the dogu.
 *  Changes of the '/dogu' directory will be noticed and registeredServices updated.
 *  Every service will be accepted if the ecosystem is in development stage.
 * 
 * @author Michael Behlendorf
 */
public class EtcdServiceRegistryDao implements ServiceRegistryDao {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdServiceRegistryDao.class);
    
    private List<RegisteredService> registeredServices;

    private String fqdn;

    private final List<String> allowedAttributes;

    private final JSONParser parser = new JSONParser();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    EtcdServiceRegistryDao(List allowedAttributes) throws IOException, EtcdException, EtcdAuthenticationException, ParseException, TimeoutException {

        this.allowedAttributes = allowedAttributes;
        registeredServices = new ArrayList<>();
        EtcdClient etcd = new EtcdClient(URI.create(getEtcdUri()));
        EtcdKeysResponse stageResponse = etcd.get("/config/_global/stage").send().get();

        if (stageResponse.getNode().getValue().equals("development")) {
            addDevService();

        } else {
            EtcdResponsePromise<EtcdKeysResponse> response1 = etcd.getDir("/dogu").recursive().send();
            EtcdKeysResponse response2 = etcd.get("/config/_global/fqdn").send().get();
            fqdn = response2.getNode().getValue();
            addServices(response1.get());
            changeLoop(etcd);
        }
    }

    private void changeLoop(EtcdClient etcd) {
       
        try {
            EtcdResponsePromise responsePromise = etcd.getDir("/dogu").waitForChange().recursive().send();
            responsePromise.addListener(promise -> {
                try {
                    addServices((EtcdKeysResponse) promise.get());
                } catch (Exception ex) {
                    LOG.warn("failed to load service", ex);
                } finally {
                    changeLoop(etcd);
                }
            });
        } catch (IOException ex) {
             LOG.error("failed to load service", ex);
        }
    }

    @Override
    public RegisteredService save(RegisteredService rs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean delete(RegisteredService rs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    @Override
    public List<RegisteredService> load() {
        try {
            lock.readLock().lock();
            return registeredServices;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public RegisteredService findServiceById(long id) {
        for (final RegisteredService r : this.registeredServices) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    private long findHighestId() {
        long id = 0;

        for (final RegisteredService r : this.registeredServices) {
            if (r.getId() > id) {
                id = r.getId();
            }
        }
        return id;
    }

    private JSONObject getCurrentDoguNode(EtcdNode doguNode) throws ParseException {
        String version = "";
        JSONObject json = null;
        // get used dogu version
        for (EtcdNode leaf : doguNode.getNodes()) {
            if (leaf.getKey().equals(doguNode.getKey() + "/current")) {
                version = leaf.getValue();
            }

        }
        // empty if dogu isnt used
        if (!version.isEmpty()) {
            for (EtcdNode leaf : doguNode.getNodes()) {
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

    private RegisteredService createService(EtcdNode doguNode) throws ParseException {
        JSONObject json = getCurrentDoguNode(doguNode);
        // check if dogu needs cas
        if (hasCasDependency(json)) {
            RegisteredServiceImpl service = new RegisteredServiceImpl();
            service.setAllowedToProxy(true);
            service.setName(json.get("Name").toString());
            service.setServiceId("https://" + fqdn + "/" + json.get("Name") + "/");
            service.setId(findHighestId() + 1);
            service.setEvaluationOrder((int) service.getId());
            service.setAllowedAttributes(allowedAttributes);
            return service;
        }
        return null;
    }

    private void addServices(EtcdKeysResponse response) throws ParseException {
        ArrayList<RegisteredService> tempServices = new ArrayList<>();
        // get all dogu nodes
        for (EtcdNode doguNode : response.getNode().getNodes()) {
            RegisteredService service = createService(doguNode);
            if (service != null) {
                tempServices.add(service);
            }
        }
        try {
            lock.writeLock().lock();
            registeredServices = tempServices;
        } finally{
            lock.writeLock().unlock();
        }
    }

    private void addDevService() {
        System.out.println("In development stage");
        RegexRegisteredService regexService = new RegexRegisteredService();
        regexService.setServiceId("^(https?|imaps?)://.*");
        regexService.setId(0);
        regexService.setName("10000001");
        regexService.setAllowedToProxy(true);
        regexService.setAllowedAttributes(allowedAttributes);
        registeredServices.add(regexService);
    }
}
