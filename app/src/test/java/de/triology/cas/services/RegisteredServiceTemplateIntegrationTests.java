package de.triology.cas.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.multitenancy.TenantExtractor;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceIndexService;
import org.apereo.cas.services.ServicesManagerConfigurationContext;
import org.apereo.cas.services.mgmt.DefaultServicesManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegisteredServiceTemplateIntegrationTests {

    private static final long CAS_SERVICE_ID = 1001;
    private static final long OAUTH_SERVICE_ID = 1002;
    private static final long OIDC_SERVICE_ID = 1003;
    private static final long SECOND_OAUTH_SERVICE_ID = 1004;

    @TempDir
    Path registryDirectory;

    /**
     * Renders four registry records from the production generator templates and loads them through
     * the real serializer, template manager, registry, and CAS services manager. Manager loading
     * and direct registry lookups must return resolved identities. Required matching fields are
     * present in the generated records even before CAS applies its internal templates.
     */
    @Test
    void productionGeneratedServicesHaveResolvedIdentitiesInManagerAndRegistry() throws IOException {
        TestFixture fixture = createFixture("example[.]org");

        Collection<RegisteredService> loadedServices = fixture.servicesManager().load();

        assertEquals(4, loadedServices.size());
        loadedServices.forEach(RegisteredServiceTemplateIntegrationTests::assertResolvedIdentity);

        assertResolvedIdentity(fixture.registry().findServiceById(CAS_SERVICE_ID));
        assertResolvedIdentity(fixture.registry().findServiceById(OAUTH_SERVICE_ID));
        assertResolvedIdentity(fixture.registry().findServiceById(OIDC_SERVICE_ID));
        assertResolvedIdentity(fixture.registry().findServiceById(SECOND_OAUTH_SERVICE_ID));

    }

    /**
     * Loads generated records directly from the registry and places them in the CAS cache to
     * simulate a path that bypasses template expansion. The generated records must remain valid
     * candidates and public service matching must not throw.
     */
    @Test
    void productionGeneratedRegistryServicesDoNotBreakCasCandidateMatching() throws IOException {
        TestFixture fixture = createFixture("example[.]org");

        Collection<RegisteredService> loadedServices = fixture.registry().load();
        assertEquals(4, loadedServices.size());
        loadedServices.forEach(RegisteredServiceTemplateIntegrationTests::assertResolvedIdentity);
        loadedServices.forEach(service -> fixture.servicesCache().put(service.getId(), service));

        Service requestedService = mock(Service.class);
        when(requestedService.getId()).thenReturn("https://example.org/oauth-service");

        assertDoesNotThrow(() -> fixture.servicesManager().findServiceBy(requestedService));
    }

    private TestFixture createFixture(String fqdn) throws IOException {
        Path repositoryRoot = findRepositoryRoot();
        Path templatesDirectory = repositoryRoot.resolve("resources/etc/cas/services/templates");
        var applicationContext = mock(ConfigurableApplicationContext.class);
        var serializer = new CesRegisteredServiceJsonSerializer(applicationContext);
        var templatesManager = new CesLegacyCompatibleTemplatesManager(
                jsonFilesIn(templatesDirectory),
                serializer
        );

        writeGeneratedServices(repositoryRoot, fqdn);

        var registry = new CesAbstractResourceBasedServiceRegistry(
                registryDirectory,
                serializer,
                applicationContext,
                List.of()
        );
        @SuppressWarnings("unchecked")
        ServiceFactory<WebApplicationService> serviceFactory = mock(ServiceFactory.class);
        Cache<Long, RegisteredService> servicesCache = Caffeine.newBuilder().build();
        var configurationContext = ServicesManagerConfigurationContext.builder()
                .serviceRegistry(registry)
                .applicationContext(applicationContext)
                .environments(Set.of())
                .servicesCache(servicesCache)
                .registeredServiceLocators(List.of())
                .registeredServicesTemplatesManager(templatesManager)
                .casProperties(new CasConfigurationProperties())
                .tenantExtractor(mock(TenantExtractor.class))
                .serviceFactory(serviceFactory)
                .registeredServiceIndexService(mock(RegisteredServiceIndexService.class))
                .build();
        var servicesManager = new DefaultServicesManager(configurationContext);
        return new TestFixture(registry, servicesManager, servicesCache);
    }

    private void writeGeneratedServices(Path repositoryRoot, String fqdn) throws IOException {
        Path casTemplate = repositoryRoot.resolve("resources/etc/cas/config/services/cas-service-template.json.tpl");
        Path oauthTemplate = repositoryRoot.resolve("resources/etc/cas/config/services/oauth-service-template.json.tpl");

        writeGeneratedService(
                casTemplate,
                registryDirectory.resolve("cas-service.json"),
                CAS_SERVICE_ID,
                "cas-service",
                "BaseService,DefaultAttributeReleasePolicy,AllowProxyPolicy",
                "org.apereo.cas.services.CasRegisteredService",
                fqdn
        );
        writeGeneratedService(
                oauthTemplate,
                registryDirectory.resolve("oauth-service.json"),
                OAUTH_SERVICE_ID,
                "oauth-service",
                "BaseService,DefaultAttributeReleasePolicy,DefaultOAuthService",
                "org.apereo.cas.support.oauth.services.OAuthRegisteredService",
                fqdn
        );
        writeGeneratedService(
                oauthTemplate,
                registryDirectory.resolve("oidc-service.json"),
                OIDC_SERVICE_ID,
                "oidc-service",
                "BaseService,DefaultAttributeReleasePolicy,DefaultOAuthService",
                "org.apereo.cas.services.OidcRegisteredService",
                fqdn
        );
        writeGeneratedService(
                oauthTemplate,
                registryDirectory.resolve("second-oauth-service.json"),
                SECOND_OAUTH_SERVICE_ID,
                "second-oauth-service",
                "BaseService,DefaultAttributeReleasePolicy,DefaultOAuthService",
                "org.apereo.cas.support.oauth.services.OAuthRegisteredService",
                fqdn
        );
    }

    private static void writeGeneratedService(
            Path sourceTemplate,
            Path destination,
            long id,
            String serviceName,
            String templateNames,
            String serviceClass,
            String fqdn
    ) throws IOException {
        String json = Files.readString(sourceTemplate)
                .replace("{{SERVICE_ID}}", Long.toString(id))
                .replace("{{TEMPLATES}}", templateNames)
                .replace("{{SERVICE}}", serviceName)
                .replace("{{FQDN}}", fqdn)
                .replace("{{LOGOUT_URL}}", "https://example.org/" + serviceName + "/logout")
                .replace("{{CLIENT_SECRET_HASH}}", "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                .replace("{{SERVICE_CLASS}}", serviceClass);
        Files.writeString(destination, json);
    }

    private static List<java.io.File> jsonFilesIn(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(Path::toFile)
                    .toList();
        }
    }

    private static Path findRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("resources/etc/cas/config/services/cas-service-template.json.tpl"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate repository root from " + Path.of("").toAbsolutePath());
    }

    private static void assertResolvedIdentity(RegisteredService service) {
        assertNotNull(service);
        assertNotNull(service.getName());
        assertFalse(service.getName().isBlank());
        assertNotNull(service.getServiceId());
        assertFalse(service.getServiceId().isBlank());
    }

    private record TestFixture(
            CesAbstractResourceBasedServiceRegistry registry,
            DefaultServicesManager servicesManager,
            Cache<Long, RegisteredService> servicesCache
    ) {
    }
}
