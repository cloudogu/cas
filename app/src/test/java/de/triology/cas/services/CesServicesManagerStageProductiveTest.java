package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.Registry.DoguChangeListener;
import de.triology.cas.services.attributes.ReturnMappedAttributesPolicy;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.services.*;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesServicesManagerStageProductive}.
 */
public class CesServicesManagerStageProductiveTest {
    private static final String EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME = "fully/qualified";
    private static final String EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX = CesDoguServiceFactory.generateServiceIdFqdnRegex("fully/qualified");
    private static final CesDoguServiceFactory doguServiceFactory = new CesDoguServiceFactory();
    private static final CesOAuthServiceFactory<OAuthRegisteredService> oAuthServiceFactory = new CesOAuthServiceFactory<>(OAuthRegisteredService::new);
    private static final CesOAuthServiceFactory<OidcRegisteredService> oidcServiceFactory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
    private static final CesServiceData EXPECTED_SERVICE_DATA_1 = new CesServiceData("nexus", doguServiceFactory);
    private static final CesServiceData EXPECTED_SERVICE_DATA_2 = new CesServiceData("smeagol", doguServiceFactory);
    private static final CesServiceData EXPECTED_SERVICE_CAS = new CesServiceData("cas", doguServiceFactory);
    private static final CesServiceData EXPECTED_OAUTH_SERVICE_DATA = new CesServiceData("portainer", oAuthServiceFactory);
    private static final CesServiceData EXPECTED_OIDC_SERVICE_DATA = new CesServiceData("cas-oidc-client", oidcServiceFactory);

    private List<String> expectedAllowedAttributes = Arrays.asList("attribute a", "attribute b");
    private Map<String, String> attributesMappingRules = Map.of("attribute z", "attribute a");
    private List<ExpectedService> expectedServices;
    private Registry registry = mock(Registry.class);
    private CesServiceManagerConfiguration managerConfig = new CesServiceManagerConfiguration("stage", expectedAllowedAttributes, attributesMappingRules, false, null, "username");
    private CesServiceManagerConfiguration managerConfigWithOIDC = new CesServiceManagerConfiguration("stage", expectedAllowedAttributes, attributesMappingRules, true, "my-test-name", "username");
    private CesServicesManagerStageProductive stage =
            new CesServicesManagerStageProductive(managerConfig, registry);
    private CesServicesManagerStageProductive stageWithOIDC =
            new CesServicesManagerStageProductive(managerConfigWithOIDC, registry);

    @Before
    public void setUp() {
        when(registry.getFqdn()).thenReturn(EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME);
        when(registry.getInstalledCasServiceAccountsOfType(any(), any()))
                .thenReturn(List.of(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2));

        expectedServices = new LinkedList<>(Arrays.asList(
                new ExpectedService().name(EXPECTED_SERVICE_DATA_1.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/nexus(/.*)?")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/nexus/something"),
                new ExpectedService().name(EXPECTED_SERVICE_DATA_2.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/smeagol(/.*)?")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/smeagol/somethingElse"),
                new ExpectedService().name(EXPECTED_SERVICE_CAS.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/cas(/.*)?")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/cas/somethingElse")
        ));
    }

    /**
     * Test for listener, when a dogu is added after initialization.
     */
    @Test
    public void doguChangeListenerAddDogu() {
        // Initialize expectedServices
        DoguChangeListener doguChangeListener = initialize();

        // Add service
        String expectedServiceName3 = "scm";
        CesServiceData serviceDataSCM = new CesServiceData(expectedServiceName3, doguServiceFactory);

        doReturn(new LinkedList<>(Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2, serviceDataSCM)))
                .when(registry).getInstalledCasServiceAccountsOfType(eq(Registry.SERVICE_ACCOUNT_TYPE_CAS), any());
        expectedServices.add(new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/scm(/.*)?"));
        //Do not expect the o auth service as the attributes are missing

        // Notify manager of change
        doguChangeListener.onChange();

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();

        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    /**
     * Test for listener, when a dogu is added after initialization.
     */
    @Test
    public void managerAddDelegatedAuthenticationProvider() {
        doReturn(new LinkedList<>(Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2)))
                .when(registry).getInstalledCasServiceAccountsOfType(any(), any());

        // Check services of oidc stage
        Collection<RegisteredService> allServicesOfOIDCStage = stageWithOIDC.getRegisteredServices().values();
        for (RegisteredService expectedService : allServicesOfOIDCStage) {
            assertTrue(expectedService.getAccessStrategy() instanceof DefaultRegisteredServiceAccessStrategy);
            assertTrue(expectedService.getAccessStrategy().getDelegatedAuthenticationPolicy() instanceof DefaultRegisteredServiceDelegatedAuthenticationPolicy);
            assertTrue(expectedService.getUsernameAttributeProvider() instanceof PrincipalAttributeRegisteredServiceUsernameProvider);
            assertEquals("username", ((PrincipalAttributeRegisteredServiceUsernameProvider) expectedService.getUsernameAttributeProvider()).getUsernameAttribute());
            List<String> allowedProviders = new ArrayList<>(expectedService.getAccessStrategy().getDelegatedAuthenticationPolicy().getAllowedProviders());
            assertEquals(1, allowedProviders.size());
            assertEquals(managerConfigWithOIDC.getOidcClientDisplayName(), allowedProviders.getFirst());
        }

        // Check services of default stage
        Collection<RegisteredService> allServicesOfDefaultStage = stage.getRegisteredServices().values();
        for (RegisteredService expectedService : allServicesOfDefaultStage) {
            assertTrue(expectedService.getAccessStrategy() instanceof DefaultRegisteredServiceAccessStrategy);
            assertTrue(expectedService.getUsernameAttributeProvider() instanceof DefaultRegisteredServiceUsernameProvider);
            assertTrue(expectedService.getAccessStrategy().getDelegatedAuthenticationPolicy() instanceof DefaultRegisteredServiceDelegatedAuthenticationPolicy);
            assertNull(null, expectedService.getAccessStrategy().getDelegatedAuthenticationPolicy().getAllowedProviders());
        }
    }

    /**
     * Test for listener, when a dogu is added after initialization.
     */
    @Test
    public void doguChangeListenerAddDoguNoFail() {
        // Initialize expectedServices
        DoguChangeListener doguChangeListener = initialize();

        // Add service
        String expectedServiceName3 = "scm";
        CesServiceData serviceDataSCM = new CesServiceData(expectedServiceName3, doguServiceFactory);

        HashMap<String, String> attributes = new HashMap<>();
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, EXPECTED_OAUTH_SERVICE_DATA.getName());
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "supersecret");
        CesServiceData correctOAuthService = new CesServiceData(EXPECTED_OAUTH_SERVICE_DATA.getName(), oAuthServiceFactory, attributes);

        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, EXPECTED_OIDC_SERVICE_DATA.getName());
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "supersecret");
        CesServiceData correctOidcService = new CesServiceData(EXPECTED_OIDC_SERVICE_DATA.getName(), oidcServiceFactory, attributes);

        doReturn(new LinkedList<>(Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2, serviceDataSCM)))
                .when(registry).getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_CAS, stage.doguServiceFactory);
        doReturn(new LinkedList<>(Collections.singletonList(correctOAuthService)))
                .when(registry).getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, stage.oAuthServiceFactory);
        doReturn(new LinkedList<>(Collections.singletonList(correctOidcService)))
                .when(registry).getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OIDC, stage.oidcServiceFactory);

        expectedServices.add(new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/scm(/.*)?"));
        expectedServices.add(new ExpectedService().name(correctOAuthService.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/portainer(/.*)?"));
        expectedServices.add(new ExpectedService().name(correctOidcService.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/cas-oidc-client(/.*)?"));

        // Notify manager of change
        doguChangeListener.onChange();

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();

        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    /**
     * Test for update-method when a dogu is added after initialization.
     */
    @Test
    public void updateRegisteredServicesAddService() {
        stage.initRegisteredServices();

        // Add service
        String expectedServiceName3 = "scm";
        CesServiceData serviceDataSCM = new CesServiceData(expectedServiceName3, doguServiceFactory);

        when(registry.getInstalledCasServiceAccountsOfType(any(), any())).thenReturn(new LinkedList<>(
                Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2, serviceDataSCM)));

        ExpectedService service3 = new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME_REGEX + "(:443)?/scm(/.*)?");
        expectedServices.add(service3);

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();

        // assure if the new service is NOT already in allServices before update is executed
        service3.assertNotContainedIn(allServices);
        // do update
        stage.updateRegisteredServices();
        // now check if update was executed successful
        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    /**
     * Test for update-method without an initialization.
     * This happens in production if the user does not use cas before the first automatic update.
     */
    @Test
    public void updateRegisteredServicesWithoutInit() {
        stage.updateRegisteredServices();

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();

        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    /**
     * Stage should still be in the same state after calling initRegisteredServices
     * a second time.
     */
    @Test
    public void initNotPerformedTwice() {
        stage.initRegisteredServices();
        stage.initRegisteredServices();

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();
        // ensures that init only happened once
        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    @Test
    public void addNewServiceWhichHasNoLogoutUri() throws GetCasLogoutUriException, CesServiceCreationException {
        // given
        RegistryEtcd etcdRegistry = mock(RegistryEtcd.class);
        CesServicesManagerStageProductive productiveStage =
                new CesServicesManagerStageProductive(managerConfig, etcdRegistry);
        GetCasLogoutUriException expectedException = new GetCasLogoutUriException("expected exception");
        when(etcdRegistry.getCasLogoutUri(any())).thenThrow(expectedException);
        CesServiceData testServiceData = new CesServiceData("testService", doguServiceFactory);
        var testService = doguServiceFactory.createNewService(
                productiveStage.createId(), EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME, null, testServiceData);

        // when
        productiveStage.addNewService(testService);

        // then
        CasRegisteredService registeredService = (CasRegisteredService) productiveStage.getRegisteredServices().get(1L);
        assertNull(registeredService.getLogoutUrl());
    }

    /**
     * Test for listener, when a dogu is removed after initialization.
     */
    @Test
    public void doguChangeListenerAddDoguRemoveDogu() {
        // Initialize expectedServices
        DoguChangeListener doguChangeListener = initialize();

        // Remove service
        when(registry.getInstalledCasServiceAccountsOfType(any(), any())).thenReturn(new LinkedList<>(Collections.singletonList(EXPECTED_SERVICE_DATA_2)));
        expectedServices = expectedServices.stream().filter(expectedService -> !EXPECTED_SERVICE_DATA_1.getIdentifier()
                .equals(expectedService.name)).collect(Collectors.toList());

        // Notify manager of change
        doguChangeListener.onChange();

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();
        assertEquals(expectedServices.size(), allServices.size());
        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
    }

    /**
     * Calls {@link CesServicesManagerStageProductive#getRegisteredServices()} and returns the
     * {@link DoguChangeListener} passed to {@link Registry#addDoguChangeListener(DoguChangeListener)}.
     */
    private DoguChangeListener initialize() {
        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();
        for (ExpectedService expectedService : expectedServices) {
            expectedService.assertContainedIn(allServices);
        }
        ArgumentCaptor<DoguChangeListener> doguChangeListener = ArgumentCaptor.forClass(DoguChangeListener.class);
        verify(registry).addDoguChangeListener(doguChangeListener.capture());
        return doguChangeListener.getValue();
    }

    /**
     * Helper class for storing test data and asserting services.
     */
    private class ExpectedService {
        boolean allowedToProxy = true;
        List<String> allowedAttributes = expectedAllowedAttributes;
        String name;
        String serviceId;
        String serviceIdExample;

        ExpectedService name(String name) {
            this.name = name;
            return this;
        }

        ExpectedService serviceId(String serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /**
         * An example that matches the service ID. This attrbitute is ignored in {@link #assertContainedIn(Collection)}.
         */
        ExpectedService serviceIdExample(String serviceIdExample) {
            this.serviceIdExample = serviceIdExample;
            return this;
        }

        /**
         * Asserts that a service with the specified name is contained within <code>services</code> and that this
         * service's attributes equal the one specified in this {@link ExpectedService}.
         */
        void assertContainedIn(Collection<RegisteredService> services) {
            List<RegisteredService> matchingServices =
                    services.stream().filter(registeredService -> name.equals(registeredService.getName()))
                            .collect(Collectors.toList());
            Assert.assertEquals("Unexpected amount of services matching name=\"" + name + "\" found within services "
                    + services, 1, matchingServices.size());
            RegisteredService actualService = matchingServices.get(0);

            assertEquals("Service \" + name \": ID is not unique", 1, services.stream()
                    .filter(registeredService -> actualService.getId() == registeredService.getId())
                    .count());
            assertEqualsService(actualService);
        }

        /**
         * Asserts that a service with the specified name is not contained within <code>services</code>
         */
        void assertNotContainedIn(Collection<RegisteredService> services) {
            List<RegisteredService> matchingServices =
                    services.stream().filter(registeredService -> name.equals(registeredService.getName()))
                            .collect(Collectors.toList());
            Assert.assertEquals("Unexpected amount of services matching name=\"" + name + "\" found within services "
                    + services, 0, matchingServices.size());
        }

        /**
         * Asserts that this service's attributes equal the one specified in this {@link ExpectedService}.
         */
        void assertEqualsService(RegisteredService actualService) {
            assertEquals("Service \" + name \": Unexpected value allowedAttributes", allowedAttributes,
                    ((ReturnMappedAttributesPolicy) actualService.getAttributeReleasePolicy()).getAllowedAttributes());
            assertEquals("Service \" + name \": Unexpected value serviceId", serviceId,
                    actualService.getServiceId());
        }
    }
}
