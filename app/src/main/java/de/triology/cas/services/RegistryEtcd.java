package de.triology.cas.services;

import de.triology.cas.services.oauth.CesOAuthServiceFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdErrorCode;
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
class RegistryEtcd implements Registry {
    private static final JSONParser PARSER = new JSONParser();
    private static final String DOGU_DIR = "/dogu/";
    private static final String CAS_SERVICE_ACCOUNT_DIR = "/config/cas/service_accounts/";
    private static final String CAS_SERVICE_ACCOUNT_DIR_IN_DOGU = "service_accounts/";
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


    /**
     * Retrieves all CAS Services Accounts which are currently registered in etcd.
     *
     * @return a list containing the identifier for all registered service accounts of cas
     */
    @Override
    public List<CesServiceData> getInstalledOAuthCASServiceAccounts(ICesServiceFactory factory) {
        log.debug("Get CAS-OAuth service accounts from registry");
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir(CAS_SERVICE_ACCOUNT_DIR).send().get().getNode().getNodes();
            return extractOAuthClientsFromSADir(nodes, factory);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            log.error("Failed to getInstalledOAuthCASServiceAccounts: ", e);
            throw new RegistryException(e);
        }
    }

    /**
     * Iterates over all available etcd-Keys of CASs service accounts.
     *
     * @param nodesFromEtcd a list containing all child nodes of the `service_accounts` directory of the cas in the etcd
     * @return a list containing the identifier for all registered service accounts of cas
     */
    private List<CesServiceData> extractOAuthClientsFromSADir(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd, ICesServiceFactory factory) {
        log.debug("Entered extractOAuthClientsFromSADir");
        List<CesServiceData> serviceAccountNames = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode oAuthClient : nodesFromEtcd) {
            try {
                String clientID = oAuthClient.getKey().substring(CAS_SERVICE_ACCOUNT_DIR.length());
                String clientSecret = this.getCurrentOAuthClientSecret(clientID);
                HashMap<String, String> attributes = new HashMap<>();
                attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, clientID);
                attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET, clientSecret);
                serviceAccountNames.add(new CesServiceData(clientID, factory, attributes));
            } catch (RegistryException ex) {
                log.warn("registry exception occurred", ex);
            }
        }
        return serviceAccountNames;
    }

    private List<CesServiceData> extractDogusFromDoguRootDir(List<EtcdKeysResponse.EtcdNode> nodesFromEtcd, ICesServiceFactory factory) {
        log.debug("Entered extractDogusFromDoguRootDir");
        List<CesServiceData> doguServices = new ArrayList<>();
        for (EtcdKeysResponse.EtcdNode dogu : nodesFromEtcd) {
            JSONObject json;
            try {
                String doguName = dogu.getKey().substring(DOGU_DIR.length());
                json = getCurrentDoguNode(doguName);
                if (hasCasDependency(json)) {
                    doguServices.add(new CesServiceData(doguName, factory));
                }
            } catch (ParseException ex) {
                log.warn("failed to parse EtcdNode to json", ex);
            } catch (RegistryException ex) {
                log.warn("registry exception occurred", ex);
            }
        }
        return doguServices;
    }

    @Override
    public List<CesServiceData> getInstalledDogusWhichAreUsingCAS(ICesServiceFactory factory) {
        log.debug("Get Dogus from registry");
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir(DOGU_DIR).send().get().getNode().getNodes();
            return extractDogusFromDoguRootDir(nodes, factory);
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

    /**
     * Retrieves the value of a given key from the etcd. If the key does not exists then an empty string is returned.
     *
     * @param key Identifier for the wanted value
     * @return the value for the given key if present, otherwise an empty string.
     */
    public String getEtcdValueForKeyIfPresent(String key) {
        log.debug("Get " + key + " from registry");
        try {
            return etcd.get(key).send().get().getNode().getValue();
        } catch (EtcdException e) {
            if (e.isErrorCode(EtcdErrorCode.KeyNotFound)) {
                log.debug("Failed to getEtcdValueForKeyIfPresent: key \"" + key + "\" not found");
                //Valid case if key is not found return an empty string
                return "";
            } else {
                log.warn("Failed to getEtcdValueForKey: ", e);
                throw new RegistryException(e);
            }
        } catch (IOException | EtcdAuthenticationException | TimeoutException e) {
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
        String doguVersion = getEtcdValueForKeyIfPresent(DOGU_DIR + doguName + "/current");
        // empty if dogu isnt used
        if (!doguVersion.isEmpty()) {
            String doguDescription = getEtcdValueForKey(DOGU_DIR + doguName + "/" + doguVersion);
            json = (JSONObject) PARSER.parse(doguDescription);
        }
        return json;
    }

    /**
     * sanitize the clientId.
     * <p>
     * The clientId should only consists out of letters and numbers, no whitespace or other special characters.
     * We sanitize the clientId because we call doguctl in getCurrentOAuthClientSecret() with the clientId as an argument.
     *
     * @param clientID
     * @return the clientId if everything is ok, otherwise an empty string
     */
    private String sanitizeClientID(String clientID) throws IllegalArgumentException {
        if (!clientID.matches("^[a-zA-Z0-9_]*$")) {
            // clientId is "unclean"
            return "";
        }
        return clientID;
    }


    /**
     * Retrieves the client secret for a given clientId and encrypts the secret accordingly.
     *
     * @param clientID Identifier for the OAuth client
     * @return hash of the actual client secret
     * <p>
     */
    protected String getCurrentOAuthClientSecret(String clientID) {

            /*
            TODO
        // we sanitize the client id to close potential code injection attacks
        String sanitizedClientId = sanitizeClientID(clientID);
        if (sanitizedClientId == "") {
            log.error("client Id could not be sanatized, aborting getCurrentOauthClientSecret");
            return "";
        }

        try {
            //Process process = new ProcessBuilder("doguctl", "config", "-e", CAS_SERVICE_ACCOUNT_DIR_IN_DOGU + sanitizedClientId).start();
            log.info("OAuthO Start:" + "Start process for oauth: " + sanitizedClientId);
            ProcessBuilder builder = new ProcessBuilder("/usr/bin/doguctl", "config", "-e", CAS_SERVICE_ACCOUNT_DIR_IN_DOGU + sanitizedClientId);
            //ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", "doguctl config -e " + CAS_SERVICE_ACCOUNT_DIR_IN_DOGU + sanitizedClientId);

            builder.redirectInput(ProcessBuilder.Redirect.PIPE); // #
            builder.redirectErrorStream(true);
            builder.directory(new File("/")); // #
            Process process = builder.start();

            process.getInputStream().close(); // #
            process.waitFor();

            return "";

            final StringWriter writer = new StringWriter();

            new Thread(() -> {
                try {
                    IOUtils.copy(process.getInputStream(), writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            final int exitValue = process.waitFor();
            final String output = writer.toString();


            log.info("OAuthO Output: " + output);

            log.info("OAuthO Exit Code:" + exitValue);

             return output;

             TODO ProcessBuilder	inheritIO() Sets the source and destination for subprocess standard I/O to be the same as those of the current Java process.
            */


        try {
            return getEtcdValueForKeyIfPresent(CAS_SERVICE_ACCOUNT_DIR + "/" + clientID);
        } catch (Exception e) {
            log.error("Failed to read private key: ", e);
            log.error("Failed to retrieve client secret for {} : {}", clientID, e);
        }
        return "";
    }
}