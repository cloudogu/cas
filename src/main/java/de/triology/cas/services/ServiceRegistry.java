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
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import mousio.etcd4j.responses.EtcdKeysResponse.EtcdNode;

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
    
    private List<RegisteredService> registeredServices = new ArrayList<RegisteredService>();
    
    private String fqdn;
    
    ServiceRegistry() throws IOException, EtcdException, EtcdAuthenticationException, ParseException, TimeoutException{
        registeredServices = new ArrayList<RegisteredService>();
        
        EtcdClient etcd = new EtcdClient(URI.create(getEtcdUri()));
    
            
        EtcdKeysResponse response1 = etcd.getDir("/dogu").recursive().send().get();
        
        EtcdKeysResponse response2 = etcd.get("/config/_global/fqdn").send().get();
        
        fqdn = response2.getNode().getValue();
            
        addServices(response1);
    
    }
    
    
    public RegisteredService save(RegisteredService rs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean delete(RegisteredService rs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private String getEtcdUri() throws FileNotFoundException, IOException{
        BufferedReader bR;        
        // read node_master
        bR = new BufferedReader(new FileReader("/etc/ces/node_master"));
        String nodeMaster = bR.readLine();

        // empty node_master
        if(nodeMaster.equals("")){
            Logger.getLogger(ServiceRegistry.class.getName()).log(Level.SEVERE,"/etc/ces/node_master is empty");
        }
        String uri = "http://"+nodeMaster +":4001";
        return uri;
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

    private void addServices(EtcdKeysResponse response) throws ParseException {
        List<String> allowedAttributes = Arrays.asList("username","cn","mail","groups","givenName","surname","displayName");
        // get all dogu nodes
            for(EtcdNode doguNode : response.getNode().getNodes()){
                String version ="";
                // get used dogu version
                for(EtcdNode leaf : doguNode.getNodes()){   
                    if(leaf.getKey().equals(doguNode.getKey()+"/current")){
                        version = leaf.getValue();
                    }
                
                }
                // empty if dogu isnt used
                if(!version.isEmpty()){
                    for(EtcdNode leaf : doguNode.getNodes()){                    
                        if(leaf.getKey().equals(doguNode.getKey()+"/"+version)){
                            JSONParser parser = new JSONParser();
                            JSONObject json = (JSONObject) parser.parse(leaf.getValue());
                            // check if dogu needs cas
                            if(json.get("Dependencies")!=null && ((JSONArray)json.get("Dependencies")).contains("cas")) {
                                RegisteredServiceImpl rS = new RegisteredServiceImpl();
                                rS.setAllowedToProxy(true);
                                rS.setName(json.get("Name").toString());                                
                                rS.setServiceId(fqdn+json.get("Name"));
                                rS.setId(findHighestId()+1);
                                rS.setEvaluationOrder((int) rS.getId());
                                rS.setAllowedAttributes(allowedAttributes);
                                registeredServices.add(rS);
                            }                            
                        }                
                    }
                }
            }
    }
    
}
