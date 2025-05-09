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
import org.apereo.cas.authentication.principal.Principal;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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

                        Object serviceObj = model.get("service");
                        if (serviceObj instanceof WebApplicationService service) {
                            val registeredService = servicesManager.findServiceBy(service);
                            val principal = assertion.getPrimaryAuthentication().getPrincipal();

                            if (principal instanceof Principal p) {
                                Map<String, Object> principalAttributes = p.getAttributes()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> e.getValue().size() == 1 ? e.getValue().get(0) : e.getValue()
                                ));
                            
                                Map<String, Object> authnAttributes = assertion.getPrimaryAuthentication()
                                    .getAttributes()
                                    .entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().size() == 1 ? e.getValue().get(0) : e.getValue()
                                    ));

                                val attributes = protocolAttributeEncoder.encodeAttributes(
                                    authnAttributes,
                                    principalAttributes,
                                    registeredService,
                                    service
                                );

                                Map<String, Object> mappedAttributes = new HashMap<>();
                                mappedAttributes.put("login", attributes.get("username"));
                                mappedAttributes.put("mail", attributes.get("mail"));
                                mappedAttributes.put("firstname", attributes.get("givenName"));
                                mappedAttributes.put("lastname", attributes.get("surname"));

                                LOGGER.info("principal: {}", p);
                                LOGGER.info("principalId: {}", p.getId());
                                LOGGER.info("principalAttributes: {}", p.getAttributes());                                
                                LOGGER.info("attributes: {}", attributes);
                                LOGGER.info("authnAttributes: {}", authnAttributes);
                                LOGGER.info("mappedAttributes: {}", mappedAttributes);


                                Map<String, Object> mergedAttributes = new LinkedHashMap<>();

                                mergedAttributes.putAll(attributes);         // encoded & filtered attributes
                                mergedAttributes.putAll(p.getAttributes());  // raw principal attributes
                                mergedAttributes.putAll(mappedAttributes);   // explicitly mapped


                                CasProtocolAttributesRenderer attributeRenderer = attributesMap ->
                                attributesMap.entrySet().stream()
                                    .flatMap(entry -> {
                                        String name = CasProtocolAttributesRenderer.sanitizeAttributeName(entry.getKey());
                                        Object value = entry.getValue();
                                        if (value instanceof Collection<?> coll) {
                                            return coll.stream().map(val -> "<cas:" + name + ">" + val + "</cas:" + name + ">");
                                        }
                                        return Stream.of("<cas:" + name + ">" + value + "</cas:" + name + ">");
                                    })
                                    .collect(Collectors.toList());
                            
                            Collection<String> renderedAttributes = attributeRenderer.render(attributes);
                            List<String> formatted = new ArrayList<>(renderedAttributes);
                            LOGGER.info("#### formatted: {}", formatted);     

                            m.put("user", attributes.get("username"));
                            m.put("principal", p);
                            m.put("attributes", mergedAttributes);                     
                            m.put("formattedAttributes", formatted);
                                                            
                            } else { 
                                LOGGER.info("principal is not instanceof Principal");
                            }
                        }
                        else {
                            LOGGER.info("serviceObj is not instanceof WebApplicationService");
                        }
             
                        LOGGER.info("Injecting proxies into model: {}", proxies);

                        m.put("proxies", proxies);
                        m.put("_proxiesInjected", true);
                    }
                    else {
                        LOGGER.info("assertionObj not instance of Assertion");
                    }
                }
                LOGGER.info("Rendering CAS 3 success view with model: {}", model);
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
