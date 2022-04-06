package de.triology.cas.services;

import lombok.val;
import mousio.etcd4j.EtcdClient;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ChainingServicesManager;
import org.apereo.cas.services.DefaultChainingServicesManager;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.ServicesManagerExecutionPlanConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration("CesServicesSpringConfiguration")
@ComponentScan("de.triology.cas.services")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CesServicesSpringConfiguration implements ServicesManagerExecutionPlanConfigurer {
    protected static final Logger LOG = LoggerFactory.getLogger(CesServicesSpringConfiguration.class);

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
        return factory.createDefaultFactory();
    }

    public RegistryEtcd createDoguRegistry(EtcdClient etcdClient) {
        return new RegistryEtcd(etcdClient);
    }

    @Bean(name = ServicesManager.BEAN_NAME)
    public ChainingServicesManager servicesManager() {
        DefaultChainingServicesManager chain = new DefaultChainingServicesManager();
        chain.registerServiceManager(configureServicesManager());
        return chain;
    }

    @Override
    public ServicesManager configureServicesManager() {
        EtcdClient etcdClient = createEtcdClient();
        RegistryEtcd doguRegistry = createDoguRegistry(etcdClient);

        LOG.debug("------- Found attribute mappings [{}]", attributesMappingRulesString);
        Map<String, String> attributesMappingRules = propertyStringToMap(attributesMappingRulesString);
        var managerConfig = new CesServiceManagerConfiguration(stage, allowedAttributes, attributesMappingRules, oidcAuthenticationDelegationEnabled, oidcClientName, oidcPrincipalsAttribute);
        return new CesServicesManager(managerConfig, doguRegistry);
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