package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesServiceFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistryLocal implements Registry{

    private static final String LOCAL_CONFIG_FILE = "/var/ces/config/local.yaml";
    private static final String GLOBAL_CONFIG_FILE = "/var/ces/config/global/config.yaml";

    @Getter
    @Setter
    private static class LocalConfig {
        private ServiceAccounts service_accounts;
    }

    @Getter
    @Setter
    private static class ServiceAccounts {
        private Map<String, String> cas;
        private Map<String, String> oidc;
        private Map<String, String> oauth;

        List<CesServiceData> getByType(String serviceAccountType, CesServiceFactory factory) {
            return switch (CasServiceAccountTypes.getByString(serviceAccountType)) {
                case OIDC -> extractServiceData(this.oidc, factory, true);
                case OAUTH -> extractServiceData(this.oauth, factory, true);
                case CAS -> extractServiceData(this.cas, factory, false);
                default ->
                        throw new RuntimeException(String.format("unknown service account type %s", serviceAccountType));
            };
        }

        private static List<CesServiceData> extractServiceData(Map<String, String> serviceAccounts, CesServiceFactory factory, boolean oauthAttributes) {
            List<CesServiceData> serviceDataList = new ArrayList<>();

            for (var serviceAccount : serviceAccounts.entrySet()) {
                var clientID = serviceAccount.getKey();
                var clientSecret = serviceAccount.getValue();

                HashMap<String, String> attributes = new HashMap<>();

                if (oauthAttributes) {
                    attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, clientID);
                    attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, clientSecret);
                }

                serviceDataList.add(new CesServiceData(clientID, factory, attributes));
            }

            return serviceDataList;
        }
    }

    @Override
    public List<CesServiceData> getInstalledCasServiceAccountsOfType(String serviceAccountType, CesServiceFactory factory) {
        var localConfigFile = new File(LOCAL_CONFIG_FILE);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(localConfigFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        ServiceAccounts serviceAccounts = parseServiceAccounts(fis);
        return serviceAccounts.getByType(serviceAccountType, factory);
    }

    // TODO: FQDN auslagern
    @Override
    public String getFqdn() {
        return null;
    }

    @Override
    public URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException {
        return null;
    }

    @Override
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {

    }

    private static ServiceAccounts parseServiceAccounts(InputStream yamlStream) {
        var yaml = new Yaml(new Constructor(LocalConfig.class, new LoaderOptions()));
        LocalConfig config = yaml.load(yamlStream);
        return config.getService_accounts();
    }
}
