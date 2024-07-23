package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import mousio.etcd4j.EtcdClient;
import org.apereo.cas.authentication.principal.ServiceMatchingStrategy;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.*;
import org.apereo.cas.services.mgmt.DefaultChainingServicesManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration("CesServicesSpringConfiguration")
@ComponentScan("de.triology.cas.services")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesServicesSpringConfiguration implements ServicesManagerExecutionPlanConfigurer {

    @Value("${ces.services.stage:production}")
    private String stage;

    @Value("${ces.services.allowedAttributes}")
    private List<String> allowedAttributes;

    @Value("${ces.services.attributeMapping:#{\"\"}}")
    private String attributesMappingRulesString;

    @Value("${ces.services.oidcPrincipalsAttribute:#{\"\"}}")
    private String oidcPrincipalsAttribute;

    @Value("${cas.authn.pac4j.oidc[0].generic.enabled:#{false}}")
    private boolean oidcAuthenticationDelegationEnabled;

    @Value("${cas.authn.pac4j.oidc[0].generic.client-name:#{\"\"}}")
    private String oidcClientName;

    public EtcdClient createEtcdClient() {
        EtcdClientFactory factory = new EtcdClientFactory();
        return factory.createDefaultClient();
    }

    public Registry createEtcdRegistry(EtcdClient etcdClient) {
        return new RegistryEtcd(etcdClient);
    }

    public Registry createLocalRegistry() {
        return new RegistryLocal();
    }

    @Bean(name = ServicesManager.BEAN_NAME)
    public ChainingServicesManager servicesManager() {
        DefaultChainingServicesManager chain = new DefaultChainingServicesManager();
        chain.registerServiceManager(configureServicesManager());
        return chain;
    }

    @Bean(name = "serviceMatchingStrategy")
    public ServiceMatchingStrategy serviceMatchingStrategy() {
        return new CesServiceMatchingStrategy();
    }

    @Override
    public ServicesManager configureServicesManager() {
        Registry registry;
        if (isMultinode()) {
            registry = createLocalRegistry();
        } else {
            EtcdClient etcdClient = createEtcdClient();
            registry = createEtcdRegistry(etcdClient);
        }

        LOGGER.debug("------- Found attribute mappings [{}]", attributesMappingRulesString);
        Map<String, String> attributesMappingRules = propertyStringToMap(attributesMappingRulesString);
        var managerConfig = new CesServiceManagerConfiguration(stage, allowedAttributes, attributesMappingRules, oidcAuthenticationDelegationEnabled, oidcClientName, oidcPrincipalsAttribute);
        return new CesServicesManager(managerConfig, registry);
    }

    private static boolean isMultinode() {
        ProcessBuilder pb = new ProcessBuilder("/usr/bin/doguctl", "multinode");
        String result;
        try {
            Process process = pb.start();
            process.waitFor();
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            while ( (line = reader.readLine()) != null) {
                builder.append(line);
            }
            result = builder.toString();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return "true".equals(result);
    }

    /**
     * Generates a map from a given property (string) of the following format:
     * this.is.my.property.key=value1:key1,value2:key2
     *
     * @param propertyString The content of the properties value
     * @return Map
     */
    public static Map<String, String> propertyStringToMap(String propertyString) {
        Map<String, String> propertyMap = new HashMap<>();
        if (propertyString.isBlank()) {
            return propertyMap;
        }

        String[] rules = propertyString.split(",");
        Arrays.stream(rules).forEach(rule -> {
            String[] keyValue = rule.split(":");
            propertyMap.put(keyValue[0], keyValue[1]);
        });
        return propertyMap;
    }
}
