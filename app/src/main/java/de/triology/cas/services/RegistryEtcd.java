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
class RegistryEtcd implements Registry {
    private static final JSONParser PARSER = new JSONParser();
    private static final String DOGU_DIR = "/dogu/";
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
    public List<String> getInstalledDogusWhichAreUsingCAS() {
        log.debug("Get Dogus from registry");
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir(DOGU_DIR).send().get().getNode().getNodes();
            return extractDogusFromDoguRootDir(nodes);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            log.error("Failed to getInstalledDogusWhichAreUsingCAS: ", e);
            throw new RegistryException(e);
        }
    }

    @Override
    public String getFqdn() {
        return getEtcdValueForKey("/config/_global/fqdn");
    }

    public String getEtcdValueForKey(String key) {
        log.debug("Get " + key + " from registry");
        try {
            return etcd.get(key).send().get().getNode().getValue();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            log.warn("Failed to getEtcdValueForKey: ", e);
            throw new RegistryException(e);
        }
    }

    public URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException {
        JSONObject doguMetaData;
        try {
            doguMetaData = getCurrentDoguNode(doguname);
            JSONObject properties;
            if (doguMetaData != null) {
                properties = getPropertiesFromMetaData(doguMetaData);
            } else {
                throw new GetCasLogoutUriException("Could not get dogu metadata");
            }
            return getLogoutUriFromProperties(properties);
        } catch (ClassCastException | NullPointerException | ParseException | URISyntaxException | RegistryException e) {
            throw new GetCasLogoutUriException(e);
        }
    }

    private URI getLogoutUriFromProperties(JSONObject properties) throws GetCasLogoutUriException, URISyntaxException {
        Object logoutUri = properties.get("logoutUri");
        if (logoutUri != null) {
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

    @Override
    @SuppressWarnings("unchecked")
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    EtcdResponsePromise responsePromise = etcd.getDir(DOGU_DIR).recursive().waitForChange().send();
                    log.info("wait for changes under /dogu");
                    responsePromise.get();
                    doguChangeListener.onChange();
                }
            } catch (IOException | EtcdException | TimeoutException | EtcdAuthenticationException e) {
                log.error("Failed to addDoguChangeListener: ", e);
                throw new RegistryException(e);
            }
        });

        t.start();
    }

    private List<String> extractDogusFromDoguRootDir(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd) {
        log.debug("Entered extractDogusFromDoguRootDir");
        List<String> stringList = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode dogu : nodesFromEtcd) {
            JSONObject json;
            try {
                String doguName = dogu.getKey().substring(DOGU_DIR.length());
                json = getCurrentDoguNode(doguName);
                if (hasCasDependency(json)) {
                    stringList.add(normalizeServiceName(json.get("Name").toString()));
                }
            } catch (ParseException ex) {
                log.warn("failed to parse EtcdNode to json", ex);
            } catch (RegistryException ex) {
                log.warn("registry exception occurred", ex);
            }
        }
        return stringList;
    }

    private String normalizeServiceName(String name) {
        String[] nameArray = StringUtils.split(name, "/");
        return nameArray[nameArray.length - 1];
    }

    private boolean hasCasDependency(JSONObject json) {
        return json != null && json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas");
    }

    protected JSONObject getCurrentDoguNode(String doguName) throws ParseException {
        JSONObject json = null;
        // get used dogu version
        String doguVersion = getEtcdValueForKey(DOGU_DIR + doguName + "/current");
        // empty if dogu isnt used
        if (!doguVersion.isEmpty()) {
            String doguDescription = getEtcdValueForKey(DOGU_DIR + doguName + "/" + doguVersion);
            json = (JSONObject) PARSER.parse(doguDescription);
        }
        return json;
    }
}