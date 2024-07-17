package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesServiceFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
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
    protected static class GlobalConfig {
        private String fqdn;
    }
    @Getter
    @Setter
    protected static class LocalConfig {
        private ServiceAccounts service_accounts = new ServiceAccounts();
    }
    @Getter
    @Setter
    protected static class ServiceAccountSecret {
        private String secret;
        private String logout_uri;
    }
    @Getter
    @Setter
    protected static class ServiceAccountCas {
        private String created;
        private String logout_uri;
    }

    @Getter
    @Setter
    protected static class ServiceAccounts {
        private Map<String, ServiceAccountCas> cas = new HashMap<>();
        private Map<String, ServiceAccountSecret> oidc = new HashMap<>();
        private Map<String, ServiceAccountSecret> oauth = new HashMap<>();

        private String getLogoutUri(String doguName) {
            HashMap<String, String> serviceAccounts = new HashMap<>();

            serviceAccounts.putAll(this.cas.entrySet().stream()
                    .filter(e -> e.getValue().logout_uri != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));
            serviceAccounts.putAll(this.oidc.entrySet().stream()
                    .filter(e -> e.getValue().logout_uri != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));
            serviceAccounts.putAll(this.oauth.entrySet().stream()
                    .filter(e -> e.getValue().logout_uri != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().logout_uri)));

            return serviceAccounts.get(doguName);
        }

        List<CesServiceData> generateByType(String serviceAccountType, CesServiceFactory factory) throws RuntimeException {
            return switch (CasServiceAccountTypes.fromString(serviceAccountType)) {
                case OIDC -> extractServiceDataSecret(this.oidc, factory);
                case OAUTH -> extractServiceDataSecret(this.oauth, factory);
                case CAS -> extractCasServiceData(this.cas, factory);
                default ->
                        throw new RegistryException(String.format("Unknown service account type %s", serviceAccountType), null);
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

        static ServiceAccounts setDefaults(ServiceAccounts serviceAccounts) {
            if (serviceAccounts == null) {
                return new ServiceAccounts();
            }

            if (serviceAccounts.cas == null) {
                serviceAccounts.cas = new HashMap<>();
            }
            if (serviceAccounts.oidc == null) {
                serviceAccounts.oidc = new HashMap<>();
            }
            if (serviceAccounts.oauth == null) {
                serviceAccounts.oauth = new HashMap<>();
            }

            return serviceAccounts;
        }
    }

    @Override
    public List<CesServiceData> getInstalledCasServiceAccountsOfType(String serviceAccountType, CesServiceFactory factory) {
        return readServiceAccounts().generateByType(serviceAccountType, factory);
    }

    ServiceAccounts readServiceAccounts() {
        try (var fis = getInputStreamForFile(LOCAL_CONFIG_FILE)) {
            var serviceAccounts = readYaml(LocalConfig.class, fis).getService_accounts();
            return ServiceAccounts.setDefaults(serviceAccounts);
        } catch (IOException e) {
            throw new RegistryException("Failed to close local config file after reading service accounts.", e);
        }
    }

    @Override
    public String getFqdn() {
        try (var fis = getInputStreamForFile(GLOBAL_CONFIG_FILE)) {
            return readYaml(GlobalConfig.class, fis).getFqdn();
        } catch (IOException e) {
            throw new RegistryException("Failed to close global config file after reading fqdn.", e);
        }
    }

    InputStream getInputStreamForFile(String path) {
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
        Thread t1 = new Thread(() -> watcher(doguChangeListener));
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

    private static <T> T readYaml(Class<T> tClass, InputStream yamlStream){
        var yaml = new Yaml(new Constructor(tClass, new LoaderOptions()));
        try {
            T result = yaml.load(yamlStream);
            if (result == null) {
                LOGGER.warn("Parsed yaml result for class {} is null; Replacing with non-null instance.", tClass.getName());
                return tClass.getDeclaredConstructor().newInstance();
            }

            return result;
        } catch (YAMLException e) {
            throw new RegistryException(String.format("Failed to parse yaml stream to class %s", tClass.getName()), e);
        } catch (InstantiationException |
                IllegalAccessException |
                InvocationTargetException |
                NoSuchMethodException e) {
            throw new RegistryException(String.format("Failed to construct new instance of %s", tClass.getName()), e);
        }
    }
}
