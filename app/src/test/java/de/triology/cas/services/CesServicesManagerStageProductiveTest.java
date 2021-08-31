package de.triology.cas.services;

import de.triology.cas.oauth.services.CesOAuthServiceFactory;
import de.triology.cas.services.Registry.DoguChangeListener;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ReturnAllowedAttributeReleasePolicy;
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
    private static final String STAGE_PRODUCTION = "production";
    private static final String EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME = "fully/qualified";
    private static final CesDoguServiceFactory doguServiceFactory = new CesDoguServiceFactory();
    private static final CesOAuthServiceFactory oAuthServiceFactory = new CesOAuthServiceFactory();
    private static final CesServiceData EXPECTED_SERVICE_DATA_1 = new CesServiceData("nexus", doguServiceFactory);
    private static final CesServiceData EXPECTED_SERVICE_DATA_2 = new CesServiceData("smeagol", doguServiceFactory);
    private static final CesServiceData EXPECTED_OAUTH_SERVICE_DATA = new CesServiceData("portainer", oAuthServiceFactory);
    private static final CesServiceData EXPECTED_SERVICE_DATA_CAS = new CesServiceData("cas", doguServiceFactory);

    private List<String> expectedAllowedAttributes = Arrays.asList("attribute a", "attribute b");
    private List<ExpectedService> expectedServices;
    private Registry registry = mock(Registry.class);
    private CesServicesManagerStageProductive stage =
            new CesServicesManagerStageProductive(expectedAllowedAttributes, registry);

    @Before
    public void setUp() {
        when(registry.getFqdn()).thenReturn(EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME);
        doReturn(new LinkedList<>(Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2)))
                .when(registry).getInstalledDogusWhichAreUsingCAS(any());

        expectedServices = new LinkedList<>(Arrays.asList(
                new ExpectedService().name(EXPECTED_SERVICE_DATA_1.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/nexus(/.*)?")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/nexus/something"),
                new ExpectedService().name(EXPECTED_SERVICE_DATA_2.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/smeagol(/.*)?")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/smeagol/somethingElse"),
                new ExpectedService().name(EXPECTED_SERVICE_DATA_CAS.getIdentifier())
                        .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/cas/.*")
                        .serviceIdExample("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "/cas/somethingCompletelyDifferent")));
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
                .when(registry).getInstalledDogusWhichAreUsingCAS(any());

        expectedServices.add(new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/scm(/.*)?"));
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

        doReturn(new LinkedList<>(Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2, serviceDataSCM)))
                .when(registry).getInstalledDogusWhichAreUsingCAS(any());
        doReturn(new LinkedList<>(Collections.singletonList(correctOAuthService)))
                .when(registry).getInstalledOAuthCASServiceAccounts(any());

        expectedServices.add(new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/scm(/.*)?"));
        expectedServices.add(new ExpectedService().name(correctOAuthService.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/portainer(/.*)?"));

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

        when(registry.getInstalledDogusWhichAreUsingCAS(any())).thenReturn(new LinkedList<>(
                Arrays.asList(EXPECTED_SERVICE_DATA_1, EXPECTED_SERVICE_DATA_2, serviceDataSCM)));

        ExpectedService service3 = new ExpectedService().name(serviceDataSCM.getIdentifier())
                .serviceId("https://" + EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME + "(:443)?/scm(/.*)?");
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
                new CesServicesManagerStageProductive(expectedAllowedAttributes, etcdRegistry);
        GetCasLogoutUriException expectedException = new GetCasLogoutUriException("expected exception");
        when(etcdRegistry.getCasLogoutUri(any())).thenThrow(expectedException);
        CesServiceData testServiceData = new CesServiceData("testService", doguServiceFactory);
        RegexRegisteredService testService = doguServiceFactory.createNewService(
                productiveStage.createId(), EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME, null, testServiceData);

        // when
        productiveStage.addNewService(testService);

        // then
        RegisteredService registeredService = productiveStage.getRegisteredServices().get(1L);
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
        when(registry.getInstalledDogusWhichAreUsingCAS(any())).thenReturn(new LinkedList<>(Collections.singletonList(EXPECTED_SERVICE_DATA_2)));
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
                    ((ReturnAllowedAttributeReleasePolicy) actualService.getAttributeReleasePolicy()).getAllowedAttributes());
            assertEquals("Service \" + name \": Unexpected value serviceId", serviceId,
                    actualService.getServiceId());
        }
    }
}