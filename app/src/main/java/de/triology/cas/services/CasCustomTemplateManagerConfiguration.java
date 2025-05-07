package de.triology.cas.services;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.ProtocolAttributeEncoder;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStore;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.services.ServiceRegistryProperties;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.apereo.cas.validation.AuthenticationAttributeReleasePolicy;
import org.apereo.cas.validation.CasProtocolAttributesRenderer;
import org.apereo.cas.validation.CasProtocolViewFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.apereo.cas.validation.Assertion;

import java.io.File;

import org.apereo.cas.services.*;
import java.nio.file.Path;
import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.View;


@Configuration("CasCustomTemplateManagerConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.services")
@Slf4j
public class CasCustomTemplateManagerConfiguration {

    @Bean(name = "cas3ServiceSuccessView")
    @RefreshScope
    public View cas3ServiceSuccessView(
        @Qualifier(AttributeDefinitionStore.BEAN_NAME) AttributeDefinitionStore attributeDefinitionStore,
        @Qualifier(AuthenticationAttributeReleasePolicy.BEAN_NAME) AuthenticationAttributeReleasePolicy authenticationAttributeReleasePolicy,
        @Qualifier("casAttributeEncoder") ProtocolAttributeEncoder protocolAttributeEncoder,
        @Qualifier(ServicesManager.BEAN_NAME) ServicesManager servicesManager,
        @Qualifier(AuthenticationServiceSelectionPlan.BEAN_NAME) AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan,
        @Qualifier("cas3ProtocolAttributesRenderer") CasProtocolAttributesRenderer cas3ProtocolAttributesRenderer,
        @Qualifier("cas3SuccessView") View mustacheView
    ) {
        return new View() {
            @Override
            public String getContentType() {
                return mustacheView.getContentType();
            }

            @Override
            public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                if (model instanceof Map m && !m.containsKey("_proxiesInjected")) {
                    Object assertionObj = m.get("assertion");
                    if (assertionObj instanceof Assertion assertion) {
                        LOGGER.info("Assertion found: {}", assertion.getClass().getSimpleName());
                        LOGGER.info("Primary authentication principal: {}", assertion.getPrimaryAuthentication().getPrincipal().getId());

                        // Add `principal` for mustache template
                        m.put("principal", assertion.getPrimaryAuthentication().getPrincipal());

                        List<String> proxies = new ArrayList<>();
                        for (val auth : assertion.getChainedAuthentications().stream().skip(1).toList()) {
                            LOGGER.info("Checking proxy authentication: {}", auth);

                            Object attr = auth.getAttributes().get("pgtUrl");
                            if (attr instanceof String s) {
                                LOGGER.info("Found proxyCallbackUrl (String): {}", s);
                                proxies.add(s);
                            } else if (attr instanceof List<?> list && !list.isEmpty()) {
                                LOGGER.info("Found proxyCallbackUrl (List): {}", list.get(0));
                                proxies.add(String.valueOf(list.get(0)));
                            } else {
                                String proxyId = auth.getPrincipal().getId();
                                LOGGER.info("No proxyCallbackUrl found, using principal instead: {}", proxyId);
                                proxies.add(proxyId);
                            }
                        }
                        
                        LOGGER.info("Injecting proxies into model: {}", proxies);
                        m.put("proxies", proxies);
                        m.put("_proxiesInjected", true);
                    }
                }

                LOGGER.info("render(): Received model with keys: {}", model.keySet());
                LOGGER.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

                mustacheView.render(model, request, response);
            }
        };
    }
    
    @Bean(name = "cas3SuccessViewDelegate")
    public View cas3SuccessViewDelegate(
        @Qualifier("casProtocolMustacheViewFactory") CasProtocolViewFactory factory,
        ConfigurableApplicationContext context
    ) {
        return factory.create(context, "protocol/3.0/casServiceValidationSuccess");
    }
    
    //Fixes: Parameter 1 of method cesDebugServiceRegistry in de.triology.cas.services.CasCustomTemplateManagerConfiguration required a bean of type 'org.apereo.cas.services.util.RegisteredServiceJsonSerializer' that could not be found.
    @Bean
    @RefreshScope
    public RegisteredServiceJsonSerializer registeredServiceJsonSerializer(
            final ConfigurableApplicationContext applicationContext
    ) {
        return new CesRegisteredServiceJsonSerializer(applicationContext);
    }

    @Bean
    @RefreshScope
    public RegisteredServicesTemplatesManager registeredServicesTemplatesManager(
            final CasConfigurationProperties casProperties,
            final RegisteredServiceJsonSerializer registeredServiceJsonSerializer
    ) {
        LOGGER.info("Overriding default RegisteredServicesTemplatesManager with CesLegacyCompatibleTemplatesManager");

        ServiceRegistryProperties serviceRegistryProperties = casProperties.getServiceRegistry();

        Collection<File> serviceTemplateResources;
        try {
            File directory = serviceRegistryProperties.getTemplates().getDirectory().getLocation().getFile();
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
            serviceTemplateResources = files != null ? Arrays.asList(files) : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.debug("Could not load template directory: {}", e.getMessage(), e);
            serviceTemplateResources = Collections.emptyList();
        }

        return new CesLegacyCompatibleTemplatesManager(serviceTemplateResources, registeredServiceJsonSerializer);
    }

    @Bean
    @RefreshScope
    public ServiceRegistry cesDebugServiceRegistry(
            final CasConfigurationProperties casProperties,
            final RegisteredServiceJsonSerializer serializer,
            final ConfigurableApplicationContext applicationContext,
            final ApplicationEventPublisher eventPublisher
    ) {
        try {
            Path location = casProperties.getServiceRegistry().getJson().getLocation().getFile().toPath();
            LOGGER.debug("Using CesDebugServiceRegistry from path: {}", location);
            return new CesAbstractResourceBasedServiceRegistry(
                    location,
                    serializer,
                    applicationContext,
                    List.of()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CesDebugServiceRegistry", e);
        }
    }

    @Bean
    @RefreshScope
    public ServiceRegistryExecutionPlanConfigurer cesDebugServiceRegistryExecutionPlanConfigurer(
            final ServiceRegistry cesDebugServiceRegistry
    ) {
        return plan -> {
            LOGGER.debug("Registering CesDebugServiceRegistry in ServiceRegistryExecutionPlan");
            plan.registerServiceRegistry(cesDebugServiceRegistry);
        };
    }
}
