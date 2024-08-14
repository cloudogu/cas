package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesServiceFactory;
import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdErrorCode;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of {@link Registry} using {@link EtcdClient}.
 * <p>
 * The Dogus are queried from etcd: Installed Dogus and the version information are stored in a directory
 * <code>/dogu/${name of dogu}/current</code>. In addition, 'cas' has to be in the dependencies of the Dogu.
 * Changes of the <code>/dogu</code> directory can be recognized using {@link #addDoguChangeListener(DoguChangeListener)}.
 */
@Slf4j
class RegistryEtcd implements Registry {
    private static final JSONParser PARSER = new JSONParser();
    private static final String DOGU_DIR = "/dogu";
    private final EtcdClient etcd;

    private static final String CAS_SERVICE_ACCOUNT_DIR = "/config/cas/service_accounts";

    /**
     * Creates a etcd client that loads its URI from <code>/etc/ces/node_master</code>.
     *
     * @throws RegistryException when the URI cannot be read
     */
    public RegistryEtcd(EtcdClient etcd) {
        this.etcd = etcd;
    }

    @Override
    public List<CesServiceData> getInstalledCasServiceAccountsOfType(String type, CesServiceFactory factory) {
        LOGGER.debug("Get [{}] service accounts from registry", type);
        try {
            if (Registry.SERVICE_ACCOUNT_TYPE_CAS.equals(type)) {
                return getInstalledDogusWhichAreUsingCAS(factory);
            }

            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir(String.format("%s/%s", CAS_SERVICE_ACCOUNT_DIR, type)).send().get().getNode().getNodes();
            return extractServiceAccountClientsByType(nodes, type, factory);
        } catch (EtcdException e) {
            if (e.isErrorCode(EtcdErrorCode.KeyNotFound)) {
                return new ArrayList<>();
            } else {
                throw new RegistryException("Failed to getInstalledCasServiceAccountsOfType: ", e);
            }
        } catch (IOException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException("Failed to getInstalledCasServiceAccountsOfType: ", e);
        }
    }

    /**
     * Iterates over all available etcd-Keys of CASs service accounts.
     *
     * @param nodesFromEtcd a list containing all child nodes of the `service_accounts` directory of the cas in the etcd
     * @param type          the type of service accounts that should be extracted
     * @return a list containing the identifier for all registered service accounts of cas
     */
    private List<CesServiceData> extractServiceAccountClientsByType(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd, String type, CesServiceFactory factory) {
        LOGGER.debug("Entered extractServiceAccountClientsByType");
        var clientPathPrefix = String.format("%s/%s/", CAS_SERVICE_ACCOUNT_DIR, type);
        List<CesServiceData> serviceDataList = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode oAuthClient : nodesFromEtcd) {
            try {
                var clientID = oAuthClient.getKey().substring(clientPathPrefix.length());
                var attributes = new HashMap<String, String>();

                var accountType = CasServiceAccountTypes.fromString(type);
                if (accountType == CasServiceAccountTypes.OIDC || accountType == CasServiceAccountTypes.OAUTH) {
                    var clientSecret = getEtcdValueForKeyIfPresent(String.format("%s%s/secret", clientPathPrefix, clientID));
                    attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, clientID);
                    attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, clientSecret);
                }

                serviceDataList.add(new CesServiceData(clientID, factory, attributes));
            } catch (RegistryException ex) {
                throw new RuntimeException("registry exception occurred ", ex);
            }
        }
        return serviceDataList;
    }

    private List<CesServiceData> extractDogusFromDoguRootDir(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd, CesServiceFactory factory) {
        LOGGER.debug("Entered extractDogusFromDoguRootDir");
        List<CesServiceData> doguServices = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode dogu : nodesFromEtcd) {
            JSONObject json;
            try {
                var doguName = dogu.getKey().substring(DOGU_DIR.length() + 1);
                json = getCurrentDoguNode(doguName);
                if (hasCasDependency(json)) {
                    doguServices.add(new CesServiceData(doguName, factory));
                }
            } catch (ParseException ex) {
                throw new RuntimeException("failed to parse EtcdNode to json: ", ex);
            } catch (RegistryException ex) {
                throw new RuntimeException("registry exception occurred: ", ex);
            }
        }
        return doguServices;
    }

    public List<CesServiceData> getInstalledDogusWhichAreUsingCAS(CesServiceFactory factory) {
        LOGGER.debug("Get Dogus from registry");
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir(DOGU_DIR).send().get().getNode().getNodes();
            return extractDogusFromDoguRootDir(nodes, factory);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException("Failed to getInstalledDogusWhichAreUsingCAS: ", e);
        }
    }

    @Override
    public String getFqdn() {
        return getEtcdValueForKey("/config/_global/fqdn");
    }

    public String getEtcdValueForKey(String key) {
        LOGGER.debug("Get {} from registry", key);
        try {
            var node = etcd.get(key).send().get().getNode();
            if (node.isDir()) {
                throw new RegistryException(String.format("Failed to getEtcdValueForKey: key %s is a directory, not a file", key), null);
            }

            return node.getValue();
        } catch (EtcdException e) {
            throw new RegistryException(String.format("Failed to getEtcdValueForKey: %s", key), e);
        } catch (IOException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException("Failed to getEtcdValueForKey: ", e);
        }
    }

    /**
     * Retrieves the value of a given key from the etcd. If the key does not exists then an empty string is returned.
     *
     * @param key Identifier for the wanted value
     * @return the value for the given key if present, otherwise an empty string.
     */
    public String getEtcdValueForKeyIfPresent(String key) {
        LOGGER.debug("Get {} from registry", key);
        try {
            var node = etcd.get(key).send().get().getNode();
            if (node.isDir()) {
                throw new RegistryException(String.format("Failed to getEtcdValueForKeyIfPresent: key %s is a directory, not a file", key), null);
            }

            return node.getValue();
        } catch (EtcdException e) {
            if (e.isErrorCode(EtcdErrorCode.KeyNotFound)) {
                LOGGER.debug("Failed to getEtcdValueForKeyIfPresent: key \"{}\" not found", key);
                //Valid case if key is not found return an empty string
                return "";
            } else {
                throw new RegistryException("Failed to getEtcdValueForKeyIfPresent: ", e);
            }
        } catch (IOException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException("Failed to getEtcdValueForKeyIfPresent: ", e);
        }
    }

    @Override
    public URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException {
        try {
            String logoutUri;
            for (var accountType : Registry.CasServiceAccountTypes.values()) {
                try {
                    logoutUri = getEtcdValueForKey(String.format("/config/cas/service_accounts/%s/%s/logout_uri", accountType.toString(), doguname));
                    if (logoutUri.isEmpty()) {
                        throw new GetCasLogoutUriException("logout_uri is empty");
                    }
                    return new URI(logoutUri);
                } catch (RegistryException ignored) {
                }
            }

            LOGGER.warn("Failed to find logout URI in service_accounts directory, falling back to dogu descriptor...");
            return getLogoutUriFromDoguDescriptor(doguname);
        } catch (URISyntaxException e) {
            throw new GetCasLogoutUriException(e);
        }
    }

    private URI getLogoutUriFromDoguDescriptor(String doguname) throws GetCasLogoutUriException, URISyntaxException {
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
        } catch (ClassCastException | NullPointerException | ParseException | RegistryException e) {
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
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    EtcdResponsePromise<EtcdKeysResponse> responsePromise = etcd.getDir(DOGU_DIR).recursive().waitForChange().send();
                    LOGGER.info("wait for changes under /dogu");
                    responsePromise.get();
                    doguChangeListener.onChange();
                }
            } catch (IOException | EtcdException | TimeoutException | EtcdAuthenticationException e) {
                throw new RegistryException("Failed to addDoguChangeListener for dogus: ", e);
            }
        });
        t.start();

        Thread t2 = new Thread(() -> {
            try {
                while (true) {
                    EtcdResponsePromise<EtcdKeysResponse> responsePromise = etcd.getDir(CAS_SERVICE_ACCOUNT_DIR).recursive().waitForChange().send();
                    LOGGER.info("wait for changes under /config/cas/service_accounts");
                    responsePromise.get();
                    doguChangeListener.onChange();
                }
            } catch (IOException | EtcdException | TimeoutException | EtcdAuthenticationException e) {
                throw new RegistryException("Failed to addDoguChangeListener for service accounts: ", e);
            }
        });
        t2.start();
    }

    private boolean hasCasDependency(JSONObject json) {
        return json != null && json.get("Dependencies") != null && ((JSONArray) json.get("Dependencies")).contains("cas");
    }

    protected JSONObject getCurrentDoguNode(String doguName) throws ParseException {
        JSONObject json = null;
        // get used dogu version
        String doguVersion = getEtcdValueForKeyIfPresent(String.format("%s/%s/current", DOGU_DIR, doguName));
        // empty if dogu isnt used
        if (!doguVersion.isEmpty()) {
            String doguDescription = getEtcdValueForKey(String.format("%s/%s/%s", DOGU_DIR, doguName, doguVersion));
            json = (JSONObject) PARSER.parse(doguDescription);
        }
        return json;
    }
}
