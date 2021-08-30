package de.triology.cas.services;

import de.triology.cas.logout.CesServiceLogoutMessageBuilder;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.ServicesManagerExecutionPlanConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration("CesServicesConfiguration")
@ComponentScan("de.triology.cas.services")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CesServicesSpringConfiguration implements ServicesManagerExecutionPlanConfigurer {
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RegistryEtcd registry;

    @Value("${ces.services.stage:production}")
    private String stage;

    @Value("${ces.services.allowedAttributes}")
    private List<String> allowedAttributes;

    @Value("${ces.services.attributeMapping:#{\"\"}}")
    private String attributesMappingRulesString;

    @Value("${cas.authn.pac4j.oidc[0].generic.enabled:#{false}}")
    private boolean oidcProviderEnabled;

    @Value("${cas.authn.pac4j.oidc[0].generic.client-name:#{\"\"}}")
    private String oidcClientName;

    @Override
    public ServicesManager configureServicesManager() {
        LOGGER.debug("------- Found attribute mappings [{}]", attributesMappingRulesString);
        Map<String, String> attributesMappingRules = propertyStringToMap(attributesMappingRulesString);
        CesServiceManagerConfiguration managerConfig = new CesServiceManagerConfiguration(stage, allowedAttributes, attributesMappingRules, oidcProviderEnabled, oidcClientName);
        return new CesServicesManager(managerConfig, registry);
    }

    @Bean
    @RefreshScope
    public SingleLogoutMessageCreator defaultSingleLogoutMessageCreator() {
        return new CesServiceLogoutMessageBuilder();
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