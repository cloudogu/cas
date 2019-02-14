package de.triology.cas.services;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of {@link Registry} using {@link EtcdClient}.
 *
 * The Dogus are queried from etcd: Installed Dogus and the version information are stored in a directory
 * <code>/dogu/${name of dogu}/current</code>. In addition, 'cas' has to be in the dependencies of the Dogu.
 * Changes of the <code>/dogu</code> directory can be recognized using {@link #addDoguChangeListener(DoguChangeListener)}.
 */
// TODO unit test? Difficult because most parts of the CAS API are final without public constructor.
class RegistryEtcd implements Registry {
    private static final JSONParser PARSER = new JSONParser();
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final EtcdClient etcd;

    /**
     * Creates a etcd client that loads its URI from <code>/etc/ces/node_master</code>.
     *
     * @throws RegistryException when the URI cannot be read
     */
    public RegistryEtcd(EtcdClient etcd) {
        this.etcd = etcd;
    }

    @Override
    public List<String> getDogus() {
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir("/dogu").recursive().send().get().getNode().getNodes();
            return convertNodesToStringList(nodes);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException(e);
        }
    }

    @Override
    public String getFqdn() {
        try {
            return etcd.get("/config/_global/fqdn").send().get().getNode().getValue();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException(e);
        }
    }

    public URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException {
        JSONObject doguMetaData;
        try {
            EtcdKeysResponse.EtcdNode node = getDoguNodeFromEtcd(doguname);
            doguMetaData = getCurrentDoguNode(node);
            JSONObject properties;
            if (doguMetaData != null) {
                properties = getPropertiesFromMetaData(doguMetaData);
            } else {
                throw new GetCasLogoutUriException("Could not get dogu metadata");
            }
            return getLogoutUriFromProperties(properties);
        } catch (ClassCastException | NullPointerException | ParseException | URISyntaxException | GetDoguNodeFromEtcdException e) {
            throw new GetCasLogoutUriException(e.toString());
        }
    }

    private URI getLogoutUriFromProperties(JSONObject properties) throws GetCasLogoutUriException, URISyntaxException {
        Object logoutUri = properties.get("logoutUri");
        if (logoutUri != null){
            String logoutUriString = logoutUri.toString();
            if (logoutUriString != null) {
                return new URI(logoutUriString);
            } else {
                throw new GetCasLogoutUriException("Could not get logoutUri from properties");
            }
        } else {
            throw new GetCasLogoutUriException("Could not get logoutUri from properties");
        }
    }

    private JSONObject getPropertiesFromMetaData(JSONObject doguMetaData) {
        Object propertiesObject = doguMetaData.get("Properties");
        if (propertiesObject != null) {
            if (propertiesObject instanceof JSONObject) {
                return (JSONObject) propertiesObject;
            } else {
                throw new ClassCastException("Properties are not in JSONObject format");
            }
        } else {
            throw new NullPointerException("No Properties are set");
        }
    }

    protected EtcdKeysResponse.EtcdNode getDoguNodeFromEtcd(String name) throws GetDoguNodeFromEtcdException {
        try {
            return etcd.getDir("/dogu/"+name).recursive().send().get().getNode();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            log.error(e.toString());
            throw new GetDoguNodeFromEtcdException();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        try {
            EtcdResponsePromise responsePromise = etcd.getDir("/dogu").recursive().waitForChange().send();
            responsePromise.addListener(promise -> {
                doguChangeListener.onChange();
                /* Register again! Why?
                 * The promise is like some kind of long polling. Once the promise is fulfilled, the connection is
                 * gone. The listener, however, expects to be called on all following  changes, so just get a new
                 * promise. */
                addDoguChangeListener(doguChangeListener);
            });
        } catch (IOException e) {
            throw new RegistryException(e);
        }
    }

    private List<String> convertNodesToStringList(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd) {
        List<String> stringList = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode entry : nodesFromEtcd) {
            JSONObject json;
            try {
                json = getCurrentDoguNode(entry);
                if (hasCasDependency(json)) {
                    stringList.add(normalizeServiceName(json.get("Name").toString()));
                }
            } catch (ParseException ex) {
                log.warn("failed to parse EtcdNode to json", ex);
            }

        }
        return stringList;
    }
    
    private String normalizeServiceName(String name){
        String[] nameArray = StringUtils.split(name, "/");
        return nameArray[nameArray.length - 1];
    }

    private boolean hasCasDependency(JSONObject json) {
        return json != null && json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas");
    }

    protected JSONObject getCurrentDoguNode(EtcdKeysResponse.EtcdNode doguNode) throws ParseException {
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

                    json = (JSONObject) PARSER.parse(leaf.getValue());
                }
            }
        }

        return json;
    }
}
