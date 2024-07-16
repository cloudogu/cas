package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesServiceFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RegistryLocal implements Registry{

    private static final String LOCAL_CONFIG_FILE = "/var/ces/config/local.yaml";
    private static final String GLOBAL_CONFIG_FILE = "/var/ces/config/global/config.yaml";

    @Getter
    @Setter
    private static class GlobalConfig {
        private String fqdn;
    }
    @Getter
    @Setter
    private static class LocalConfig {
        private ServiceAccounts service_accounts;
    }
    @Getter
    @Setter
    private static class ServiceAccountSecret {
        private String secret;
        private String logout_uri;
    }
    @Getter
    @Setter
    private static class ServiceAccountCas {
        private boolean created;
        private String logout_uri;
    }

    @Getter
    @Setter
    private static class ServiceAccounts {
        private Map<String, ServiceAccountCas> cas;
        private Map<String, ServiceAccountSecret> oidc;
        private Map<String, ServiceAccountSecret> oauth;

        private String getLogoutUri(String doguName) {
            HashMap<String, String> serviceAccounts = new HashMap<>();

            serviceAccounts.putAll(this.cas.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));
            serviceAccounts.putAll(this.oidc.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));
            serviceAccounts.putAll(this.oauth.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));

            return serviceAccounts.get(doguName);
        }

        List<CesServiceData> getByType(String serviceAccountType, CesServiceFactory factory) throws RuntimeException {
            return switch (CasServiceAccountTypes.fromString(serviceAccountType)) {
                case OIDC -> extractServiceDataSecret(this.oidc, factory);
                case OAUTH -> extractServiceDataSecret(this.oauth, factory);
                case CAS -> extractCasServiceData(this.cas, factory);
                default ->
                        throw new RuntimeException(String.format("unknown service account type %s", serviceAccountType));
            };
        }

        private static List<CesServiceData> extractServiceDataSecret(Map<String, ServiceAccountSecret> serviceAccounts, CesServiceFactory factory) {
            List<CesServiceData> serviceDataList = new ArrayList<>();

            for (var serviceAccount : serviceAccounts.entrySet()) {
                var clientID = serviceAccount.getKey();
                var clientSecret = serviceAccount.getValue().secret;

                HashMap<String, String> attributes = new HashMap<>();

                attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, clientID);
                attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, clientSecret);

                serviceDataList.add(new CesServiceData(clientID, factory, attributes));
            }

            return serviceDataList;
        }

        private static List<CesServiceData> extractCasServiceData(Map<String, ServiceAccountCas> serviceAccounts, CesServiceFactory factory) {
            List<CesServiceData> serviceDataList = new ArrayList<>();

            for (var serviceAccount : serviceAccounts.entrySet()) {
                var clientID = serviceAccount.getKey();
                serviceDataList.add(new CesServiceData(clientID, factory));
            }

            return serviceDataList;
        }
    }

    @Override
    public List<CesServiceData> getInstalledCasServiceAccountsOfType(String serviceAccountType, CesServiceFactory factory) {
        return readServiceAccounts().getByType(serviceAccountType, factory);
    }

    private static ServiceAccounts readServiceAccounts() {
        return parseServiceAccounts(getFileInputStream(LOCAL_CONFIG_FILE));
    }

    @Override
    public String getFqdn() {
        return parseFqdn(getFileInputStream(GLOBAL_CONFIG_FILE));
    }

    private static FileInputStream getFileInputStream(String path) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new RegistryException(String.format("Could not find file %s", path), e);
        }
        return fis;
    }

    @Override
    public URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException {
        var serviceAccounts = readServiceAccounts();
        try {
            String logoutUri = serviceAccounts.getLogoutUri(doguname);

            if (logoutUri != null && !logoutUri.isEmpty()){
                return new URI(logoutUri);
            } else {
                throw new GetCasLogoutUriException("Could not get logoutUri");
            }
        } catch (URISyntaxException e) {
            throw new GetCasLogoutUriException(e);
        }
    }

    @Override
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        Thread t1 = new Thread(() -> {
            watcher(doguChangeListener);
        });
        t1.start();
    }

    private void watcher(DoguChangeListener doguChangeListener) {
        Path path = Paths.get(LOCAL_CONFIG_FILE);
        try(WatchService watchService = FileSystems.getDefault().newWatchService()) {
            path.register(watchService);
            var previousServiceAccounts = readServiceAccounts();
            while (true) {
                LOGGER.info("wait for changes under {}", LOCAL_CONFIG_FILE);
                watchService.take();
                var currentServiceAccounts = readServiceAccounts();
                if (!previousServiceAccounts.equals(currentServiceAccounts)) {
                    doguChangeListener.onChange();
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RegistryException("Failed to addDoguChangeListener", e);
        }
    }

    private static ServiceAccounts parseServiceAccounts(InputStream yamlStream) {
        return readYaml(LocalConfig.class, yamlStream).getService_accounts();
    }

    private static String parseFqdn(InputStream yamlStream) {
        return readYaml(GlobalConfig.class, yamlStream).getFqdn();
    }

    private static <T> T readYaml(Class<T> tClass, InputStream yamlStream){
        var yaml = new Yaml(new Constructor(tClass, new LoaderOptions()));
        return yaml.load(yamlStream);
    }
}
