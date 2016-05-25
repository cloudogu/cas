/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.triology.cas.services;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

/**
 *
 * @author mbehlendorf
 */
public class ServiceRegistry implements ServiceRegistryDao {

    private List<RegisteredService> registeredServices;

    private String fqdn;

    List<String> allowedAttributes;

    private final JSONParser parser = new JSONParser();

    ServiceRegistry() throws IOException, EtcdException, EtcdAuthenticationException, ParseException, TimeoutException {

        allowedAttributes = Arrays.asList("username", "cn", "mail", "groups", "givenName", "surname", "displayName");
        registeredServices = new ArrayList<RegisteredService>();
        EtcdClient etcd = new EtcdClient(URI.create(getEtcdUri()));
        EtcdKeysResponse stageResponse = etcd.get("/config/_global/stage").send().get();

        if (stageResponse.getNode().getValue().equals("development")) {
            addDevService();

        } else {
            EtcdResponsePromise<EtcdKeysResponse> response1 = etcd.getDir("/dogu").recursive().send();
            EtcdKeysResponse response2 = etcd.get("/config/_global/fqdn").send().get();
            fqdn = response2.getNode().getValue();
            addServices(response1.get());
            //changeLoop(etcd);
        }
    }

    public void changeLoop(EtcdClient etcd) throws IOException {
        //EtcdResponsePromise promise = etcd.getDir("/dogu").waitForChange().recursive().send();
        // promise.addListener();
    }

    public RegisteredService save(RegisteredService rs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

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

    public List<RegisteredService> load() {
        return registeredServices;
    }

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
        if (json != null) {
            return (json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas"));
        } else {
            return false;
        }
    }

    private RegisteredServiceImpl createService(EtcdNode doguNode) throws ParseException {
        JSONObject json = getCurrentDoguNode(doguNode);
        // check if dogu needs cas
        if (hasCasDependency(json)) {
            RegisteredServiceImpl rS = new RegisteredServiceImpl();
            rS.setAllowedToProxy(true);
            rS.setName(json.get("Name").toString());
            rS.setServiceId("https://" + fqdn + "/" + json.get("Name") + "/");
            rS.setId(findHighestId() + 1);
            rS.setEvaluationOrder((int) rS.getId());
            rS.setAllowedAttributes(allowedAttributes);
            return rS;
        }

        return null;

    }

    private void addServices(EtcdKeysResponse response) throws ParseException {
        // get all dogu nodes
        for (EtcdNode doguNode : response.getNode().getNodes()) {
            RegisteredServiceImpl service = createService(doguNode);
            if (service != null) {
                registeredServices.add(service);
            }
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
