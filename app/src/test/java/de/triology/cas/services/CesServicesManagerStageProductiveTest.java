package de.triology.cas.services;

import de.triology.cas.services.Registry.DoguChangeListener;
import org.jasig.cas.services.RegisteredService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CesServicesManagerStageProductive}.
 */
public class CesServicesManagerStageProductiveTest {
    private static final String STAGE_PRODUCTION = "production";
    private static final String EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME = "fully/qualified";
    private static final String EXPECTED_SERVICE_NAME_1 = "nexus";
    private static final String EXPECTED_SERVICE_NAME_2 = "smeagol";
    private static final String EXPECTED_SERVICE_NAME_CAS = "cas";
    private List<String> expectedAllowedAttributes = Arrays.asList("attribute a", "attribute b");
    private List<ExpectedService> expectedServices;
    private Registry registry = mock(Registry.class);
    private CesServicesManagerStageProductive stage =
            new CesServicesManagerStageProductive(expectedAllowedAttributes, registry);

    @Before
    public void setUp() {
        when(registry.getFqdn()).thenReturn(EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME);
        when(registry.getDogus()).thenReturn(Arrays.asList(EXPECTED_SERVICE_NAME_1, EXPECTED_SERVICE_NAME_2));
        expectedServices = new LinkedList<>(Arrays.asList(
                new ExpectedService().name(EXPECTED_SERVICE_NAME_1)
                                     .serviceId("https://fully/qualified(:443)?/nexus(/.*)?")
                                     .serviceIdExample("https://fully/qualified/nexus/something"),
                new ExpectedService().name(EXPECTED_SERVICE_NAME_2)
                                     .serviceId("https://fully/qualified(:443)?/smeagol(/.*)?")
                                     .serviceIdExample("https://fully/qualified/smeagol/somethingElse"),
                new ExpectedService().name(EXPECTED_SERVICE_NAME_CAS)
                                     .serviceId("https://fully/qualified/cas/.*")
                                     .serviceIdExample("https://fully/qualified/cas/somethingCompletelyDifferent")));
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
        when(registry.getDogus()).thenReturn(new LinkedList<>(
                Arrays.asList(EXPECTED_SERVICE_NAME_1, EXPECTED_SERVICE_NAME_2, expectedServiceName3)));
        expectedServices.add(new ExpectedService().name(expectedServiceName3)
                                                  .serviceId("https://fully/qualified(:443)?/scm(/.*)?"));
        
        
        

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
        when(registry.getDogus()).thenReturn(new LinkedList<>(
                Arrays.asList(EXPECTED_SERVICE_NAME_1, EXPECTED_SERVICE_NAME_2, expectedServiceName3)));
        ExpectedService service3 = new ExpectedService().name(expectedServiceName3)
                .serviceId("https://"+EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME+"(:443)?/scm(/.*)?");
        expectedServices.add(service3);

        Collection<RegisteredService> allServices = stage.getRegisteredServices().values();

        service3.assertNotContainedIn(allServices);

        stage.updateRegisteredServices();

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
    public void addNewServiceWhichHasNoLogoutUri() throws GetCasLogoutUriException {
        RegistryEtcd etcdRegistry = mock(RegistryEtcd.class);
        CesServicesManagerStageProductive productiveStage =
                new CesServicesManagerStageProductive(expectedAllowedAttributes, etcdRegistry);
        when(etcdRegistry.getCasLogoutUri(any())).thenThrow(new GetCasLogoutUriException("expected exception"));
        productiveStage.addNewService("testService");
    }

    /**
     * Test for listener, when a dogu is removed after initialization.
     */
    @Test
    public void doguChangeListenerAddDoguRemoveDogu() {
        // Initialize expectedServices
        DoguChangeListener doguChangeListener = initialize();
        // Remove service
        when(registry.getDogus()).thenReturn(new LinkedList<>(Collections.singletonList(EXPECTED_SERVICE_NAME_1)));
        expectedServices = expectedServices.stream().filter(expectedService -> !EXPECTED_SERVICE_NAME_2
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

            assertTrue("Service \" + name \": ID is not unique",
                       1 == services.stream()
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
            assertEquals("Service \" + name \": Unexpected value allowedToProxy", allowedToProxy,
                         actualService.isAllowedToProxy());
            assertEquals("Service \" + name \": Unexpected value allowedAttributes", allowedAttributes,
                         actualService.getAllowedAttributes());
            assertEquals("Service \" + name \": Unexpected value serviceId", serviceId,
                         actualService.getServiceId());
        }
    }
}