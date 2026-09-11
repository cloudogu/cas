package de.triology.cas.services;

import org.apereo.cas.authentication.Authentication;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.ProtocolAttributeEncoder;
import org.apereo.cas.authentication.attribute.AttributeDefinitionStore;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.services.ServiceRegistryProperties;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.ServiceRegistryExecutionPlan;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.util.RegisteredServiceJsonSerializer;
import org.apereo.cas.validation.Assertion;
import org.apereo.cas.validation.AuthenticationAttributeReleasePolicy;
import org.apereo.cas.validation.CasProtocolAttributesRenderer;
import org.apereo.cas.validation.CasProtocolViewFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.View;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.apereo.cas.configuration.model.support.services.json.JsonServiceRegistryProperties;

class CasCustomTemplateManagerConfigurationTests {

    private CasCustomTemplateManagerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new CasCustomTemplateManagerConfiguration();
    }

    @Test
    void registeredServiceJsonSerializer_ShouldReturnInstance() {
        var applicationContext = mock(ConfigurableApplicationContext.class);

        var serializer = configuration.registeredServiceJsonSerializer(applicationContext);

        assertNotNull(serializer);
        assertTrue(serializer instanceof CesRegisteredServiceJsonSerializer);
    }

    @Test
    void registeredServicesTemplatesManager_ShouldHandleExceptionGracefully() {
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getTemplates()).thenThrow(new RuntimeException("Test Exception"));

        var serializer = mock(RegisteredServiceJsonSerializer.class);

        var manager = configuration.registeredServicesTemplatesManager(casProperties, serializer);

        assertNotNull(manager);
        assertTrue(manager instanceof CesLegacyCompatibleTemplatesManager);
    }

    @Test
    void cesDebugServiceRegistry_ShouldReturnInstance() throws Exception {
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        var jsonProps = mock(JsonServiceRegistryProperties.class);

        // Create real temp directory instead of file
        File tempDir = Files.createTempDirectory("cas").toFile();
        var resource = mock(org.springframework.core.io.Resource.class);
        when(resource.getFile()).thenReturn(tempDir);

        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getJson()).thenReturn(jsonProps);
        when(jsonProps.getLocation()).thenReturn(resource);

        var serializer = mock(RegisteredServiceJsonSerializer.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var eventPublisher = mock(ApplicationEventPublisher.class);

        var registry = configuration.cesDebugServiceRegistry(casProperties, serializer, applicationContext, eventPublisher);

        assertNotNull(registry);
        assertTrue(registry instanceof CesAbstractResourceBasedServiceRegistry);

        tempDir.delete();
    }

    @Test
    void cesDebugServiceRegistryExecutionPlanConfigurer_ShouldRegisterServiceRegistryWhenInvoked() {
        var serviceRegistry = mock(ServiceRegistry.class);
        var plan = mock(ServiceRegistryExecutionPlan.class);

        var configurer = configuration.cesDebugServiceRegistryExecutionPlanConfigurer(serviceRegistry);
        assertNotNull(configurer);

        configurer.configureServiceRegistry(plan);

        verify(plan).registerServiceRegistry(serviceRegistry);
    }

    @Test
    void registeredServicesTemplatesManager_ShouldFindJsonFiles_WhenDirectoryHasFiles() throws Exception {
        // Capture configuration logs while a directory containing one template and one unrelated
        // file is scanned. The log must list the discovered JSON template and omit the ignored file.
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        var templates = mock(org.apereo.cas.configuration.model.core.templates.ServiceRegistryTemplatesProperties.class);
        var springResourceProperties = mock(org.apereo.cas.configuration.model.SpringResourceProperties.class);
        var resource = mock(org.springframework.core.io.Resource.class);

        File tempDir = Files.createTempDirectory("cas-templates").toFile();
        File jsonFile = new File(tempDir, "template.json");
        Files.writeString(jsonFile.toPath(), "{}");
        File notJsonFile = new File(tempDir, "readme.txt");
        Files.writeString(notJsonFile.toPath(), "ignored");

        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getTemplates()).thenReturn(templates);
        when(templates.getDirectory()).thenReturn(springResourceProperties);
        when(springResourceProperties.getLocation()).thenReturn(resource);
        when(resource.getFile()).thenReturn(tempDir);

        var serializer = mock(RegisteredServiceJsonSerializer.class);

        try (var logs = TestLogCapture.start()) {
            var manager = configuration.registeredServicesTemplatesManager(casProperties, serializer);

            assertNotNull(manager);
            assertTrue(manager instanceof CesLegacyCompatibleTemplatesManager);
            String discoveryLog = logs.events().stream()
                    .map(event -> event.getMessage().getFormattedMessage())
                    .filter(message -> message.startsWith("Discovered 1 registered-service template definition file"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected the template discovery log"));
            assertTrue(discoveryLog.contains(jsonFile.getAbsolutePath()));
            assertFalse(discoveryLog.contains(notJsonFile.getAbsolutePath()));
        } finally {
            jsonFile.delete();
            notJsonFile.delete();
            tempDir.delete();
        }
    }

    @Test
    void cesDebugServiceRegistry_ShouldThrow_WhenLocationCannotBeResolved() throws Exception {
        var casProperties = mock(CasConfigurationProperties.class);
        var serviceRegistryProperties = mock(ServiceRegistryProperties.class);
        var jsonProps = mock(JsonServiceRegistryProperties.class);
        var resource = mock(org.springframework.core.io.Resource.class);

        when(casProperties.getServiceRegistry()).thenReturn(serviceRegistryProperties);
        when(serviceRegistryProperties.getJson()).thenReturn(jsonProps);
        when(jsonProps.getLocation()).thenReturn(resource);
        when(resource.getFile()).thenThrow(new RuntimeException("no file"));

        var serializer = mock(RegisteredServiceJsonSerializer.class);
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var eventPublisher = mock(ApplicationEventPublisher.class);

        assertThrows(IllegalStateException.class, () ->
                configuration.cesDebugServiceRegistry(casProperties, serializer, applicationContext, eventPublisher));
    }

    @Test
    void cas3SuccessViewDelegate_ShouldDelegateToFactory() {
        var factory = mock(CasProtocolViewFactory.class);
        var context = mock(ConfigurableApplicationContext.class);
        var expectedView = mock(View.class);
        when(factory.create(context, "protocol/3.0/casServiceValidationSuccess")).thenReturn(expectedView);

        var result = configuration.cas3SuccessViewDelegate(factory, context);

        assertSame(expectedView, result);
    }

    // --- cas3ServiceSuccessView -----------------------------------------------------------

    private AttributeDefinitionStore attributeDefinitionStore;
    private AuthenticationAttributeReleasePolicy authenticationAttributeReleasePolicy;
    private ProtocolAttributeEncoder protocolAttributeEncoder;
    private ServicesManager servicesManager;
    private AuthenticationServiceSelectionPlan authenticationServiceSelectionPlan;
    private CasProtocolAttributesRenderer cas3ProtocolAttributesRenderer;
    private View mustacheView;
    private View cas3View;

    private void setUpCas3View() {
        attributeDefinitionStore = mock(AttributeDefinitionStore.class);
        authenticationAttributeReleasePolicy = mock(AuthenticationAttributeReleasePolicy.class);
        protocolAttributeEncoder = mock(ProtocolAttributeEncoder.class);
        servicesManager = mock(ServicesManager.class);
        authenticationServiceSelectionPlan = mock(AuthenticationServiceSelectionPlan.class);
        cas3ProtocolAttributesRenderer = mock(CasProtocolAttributesRenderer.class);
        mustacheView = mock(View.class);

        cas3View = configuration.cas3ServiceSuccessView(
                attributeDefinitionStore,
                authenticationAttributeReleasePolicy,
                protocolAttributeEncoder,
                servicesManager,
                authenticationServiceSelectionPlan,
                cas3ProtocolAttributesRenderer,
                mustacheView
        );
    }

    @Test
    void cas3ServiceSuccessView_getContentType_DelegatesToMustacheView() {
        setUpCas3View();
        when(mustacheView.getContentType()).thenReturn("text/xml");

        assertEquals("text/xml", cas3View.getContentType());
    }

    @Test
    void cas3ServiceSuccessView_render_ShortCircuitsWhenProxiesAlreadyInjected() throws Exception {
        setUpCas3View();
        Map<String, Object> model = new HashMap<>();
        model.put("_proxiesInjected", true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        cas3View.render(model, request, response);

        assertFalse(model.containsKey("proxies"));
        verify(mustacheView).render(model, request, response);
    }

    @Test
    void cas3ServiceSuccessView_render_HandlesAssertionMissing() throws Exception {
        setUpCas3View();
        Map<String, Object> model = new HashMap<>();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        cas3View.render(model, request, response);

        assertFalse(model.containsKey("_proxiesInjected"));
        assertFalse(model.containsKey("proxies"));
        verify(mustacheView).render(model, request, response);
    }

    @Test
    void cas3ServiceSuccessView_render_HandlesServiceNotWebApplicationService() throws Exception {
        setUpCas3View();
        Assertion assertion = mock(Assertion.class);
        when(assertion.getChainedAuthentications()).thenReturn(List.of());

        Map<String, Object> model = new HashMap<>();
        model.put("assertion", assertion);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        cas3View.render(model, request, response);

        assertEquals(true, model.get("_proxiesInjected"));
        assertEquals(List.of(), model.get("proxies"));
        assertFalse(model.containsKey("user"));
        verify(mustacheView).render(model, request, response);
    }

    @Test
    void cas3ServiceSuccessView_render_HandlesNullPrincipal() throws Exception {
        setUpCas3View();
        Assertion assertion = mock(Assertion.class);
        when(assertion.getChainedAuthentications()).thenReturn(List.of());
        Authentication primaryAuthentication = mock(Authentication.class);
        when(assertion.getPrimaryAuthentication()).thenReturn(primaryAuthentication);
        // getPrincipal() left unstubbed -> null, so "principal instanceof Principal" is false

        WebApplicationService service = mock(WebApplicationService.class);

        Map<String, Object> model = new HashMap<>();
        model.put("assertion", assertion);
        model.put("service", service);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        cas3View.render(model, request, response);

        assertFalse(model.containsKey("user"));
        assertFalse(model.containsKey("attributes"));
        assertEquals(true, model.get("_proxiesInjected"));
        verify(mustacheView).render(model, request, response);
    }

    @Test
    void cas3ServiceSuccessView_render_FullHappyPathWithProxiesAndAttributes() throws Exception {
        setUpCas3View();

        // index 0 = primary hop (skipped),
        // 1 = valid https proxy, 2 = no pgtUrl, 3 = non-http(s) scheme
        Authentication primaryHop = mock(Authentication.class);
        when(primaryHop.getAttributes()).thenReturn(Map.of());

        Authentication validProxy = mock(Authentication.class);
        when(validProxy.getAttributes()).thenReturn(Map.of("pgtUrl", List.of("https://proxy.example.com")));

        Authentication noPgtUrl = mock(Authentication.class);
        when(noPgtUrl.getAttributes()).thenReturn(Map.of());

        Authentication nonHttpProxy = mock(Authentication.class);
        when(nonHttpProxy.getAttributes()).thenReturn(Map.of("pgtUrl", List.of("ftp://not-allowed.example.com")));

        Assertion assertion = mock(Assertion.class);
        when(assertion.getChainedAuthentications()).thenReturn(List.of(primaryHop, validProxy, noPgtUrl, nonHttpProxy));

        Authentication primaryAuthentication = mock(Authentication.class);
        Map<String, List<Object>> authnAttributes = Map.of("authnAttr", List.of("authnValue"));
        when(primaryAuthentication.getAttributes()).thenReturn(authnAttributes);
        when(assertion.getPrimaryAuthentication()).thenReturn(primaryAuthentication);

        Principal principal = mock(Principal.class);
        Map<String, List<Object>> principalAttributes = Map.of("mail", List.of("alice@example.com"));
        when(principal.getAttributes()).thenReturn(principalAttributes);
        when(primaryAuthentication.getPrincipal()).thenReturn(principal);

        WebApplicationService service = mock(WebApplicationService.class);
        RegisteredService registeredService = mock(RegisteredService.class);
        when(servicesManager.findServiceBy(service)).thenReturn(registeredService);

        Map<String, Object> encoded = new HashMap<>();
        encoded.put("username", "alice");
        encoded.put("mail", "alice@example.com");
        encoded.put("givenName", "Alice");
        encoded.put("surname", "Smith");
        encoded.put("groups", List.of("admin", "user"));
        when(protocolAttributeEncoder.encodeAttributes(anyMap(), anyMap(), eq(registeredService), eq(service)))
                .thenReturn(encoded);

        Map<String, Object> model = new HashMap<>();
        model.put("assertion", assertion);
        model.put("service", service);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        cas3View.render(model, request, response);

        assertEquals("alice", model.get("user"));
        assertSame(principal, model.get("principal"));
        assertTrue(model.get("attributes") instanceof Map);
        assertTrue(model.get("formattedAttributes") instanceof List);
        assertFalse(((List<?>) model.get("formattedAttributes")).isEmpty());
        assertEquals(List.of("https://proxy.example.com"), model.get("proxies"));
        assertEquals(true, model.get("_proxiesInjected"));
        verify(mustacheView).render(model, request, response);
    }
}
