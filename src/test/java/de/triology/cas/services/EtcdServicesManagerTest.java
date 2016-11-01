package de.triology.cas.services;

import de.triology.cas.services.CloudoguRegistry.DoguChangeListener;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.org.lidalia.slf4jtest.LoggingEvent.info;

/**
 * Tests for {@link EtcdServicesManager}
 */
@RunWith(Enclosed.class)
public class EtcdServicesManagerTest {

    /**
     * Generals tests, independent of mode.
     */
    public static class General {
        EtcdServicesManager etcdServicesManger =
                new EtcdServicesManager(null, "don't care", mock(CloudoguRegistry.class));

        /**
         * Logger of class under test.
         */
        private static final TestLogger LOG = TestLoggerFactory.getTestLogger(EtcdServicesManager.class);

        /**
         * Reset logger before each test.
         **/
        @Rule public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

        /**
         * Rule for asserting exceptions.
         */
        @Rule public ExpectedException thrown = ExpectedException.none();

        /**
         * Test for {@link EtcdServicesManager#reload()}.
         */
        @Test
        public void reload() throws Exception {
            etcdServicesManger.reload();

            assertThat(LOG.getLoggingEvents(),
                       is(Collections.singletonList(info("Cas wants to reload registered services."))));
        }

        /**
         * Test for {@link EtcdServicesManager#save(RegisteredService)}.
         */
        @Test
        public void save() throws Exception {
            thrown.expect(UnsupportedOperationException.class);
            thrown.expectMessage("save");

            etcdServicesManger.save(mock(RegisteredService.class));
        }

        /**
         * Test for {@link EtcdServicesManager#delete(long)}.
         */
        @Test
        public void delete() throws Exception {
            thrown.expect(UnsupportedOperationException.class);
            thrown.expectMessage("delete");

            etcdServicesManger.delete(42L);
        }
    }

    /**
     * Tests for production mode.
     */
    public static class ProductionMode {
        static final String STAGE_PRODUCTION = "production";
        static final String EXPECTED_FULLY_QUALIFIED_DOMAIN_NAME = "fully/qualified";
        static final String EXPECTED_SERVICE_NAME_1 = "/dogu/nexus";
        static final String EXPECTED_SERVICE_NAME_2 = "/dogu/smeagol";
        static final String EXPECTED_SERVICE_NAME_CAS = "cas";
        List<String> expectedAllowedAttributes = Arrays.asList("attribute a", "attribute b");
        List<ExpectedService> expectedServices;
        CloudoguRegistry registry = mock(CloudoguRegistry.class);

        EtcdServicesManager etcdServicesManger =
                new EtcdServicesManager(expectedAllowedAttributes, STAGE_PRODUCTION, registry);

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
        public void doguChangeListenerAddDogu() throws Exception {
            // Initialize expectedServices
            DoguChangeListener doguChangeListener = assertGetAllServices();

            // Add service
            String expectedServiceName3 = "/dogu/scm";
            when(registry.getDogus()).thenReturn(new LinkedList<>(
                    Arrays.asList(EXPECTED_SERVICE_NAME_1, EXPECTED_SERVICE_NAME_2, expectedServiceName3)));
            expectedServices.add(new ExpectedService().name(expectedServiceName3)
                                                      .serviceId("https://fully/qualified(:443)?/scm(/.*)?"));

            // Notify manager of change
            doguChangeListener.onChange();

            Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
            for (ExpectedService expectedService : expectedServices) {
                expectedService.assertContainedIn(allServices);
            }
        }

        /**
         * Test for listener, when a dogu is removed after initialization.
         */
        @Test
        public void doguChangeListenerAddDoguRemoveDogu() throws Exception {
            // Initialize expectedServices
            DoguChangeListener doguChangeListener = assertGetAllServices();
            // Remove service
            when(registry.getDogus()).thenReturn(new LinkedList<>(Collections.singletonList(EXPECTED_SERVICE_NAME_1)));
            expectedServices = expectedServices.stream().filter(expectedService -> !EXPECTED_SERVICE_NAME_2
                    .equals(expectedService.name)).collect(Collectors.toList());

            // Notify manager of change
            doguChangeListener.onChange();

            Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
            for (ExpectedService expectedService : expectedServices) {
                expectedService.assertContainedIn(allServices);
            }
        }

        /**
         * Test for {@link EtcdServicesManager#getAllServices()}.
         */
        @Test
        public void getAllServices() throws Exception {
            assertGetAllServices();
        }

        /**
         * Test for {@link EtcdServicesManager#matchesExistingService(Service)}.
         */
        @Test
        public void matchesExistingService() throws Exception {
            for (ExpectedService expectedService : expectedServices) {
                Service service = mock(Service.class);
                when(service.getId()).thenReturn(expectedService.serviceIdExample);
                assertTrue("Unexpected value for service ID=" + expectedService.serviceIdExample,
                           etcdServicesManger.matchesExistingService(service));
            }
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} for a services that does not exist.
         */
        @Test
        public void matchesExistingServiceNegative() throws Exception {
            Service service = mock(Service.class);
            when(service.getId()).thenReturn("https://somethingThatDoesNotExist");
            assertFalse("Unexpected value for non-https service", etcdServicesManger.matchesExistingService(service));
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)}.
         */
        @Test
        public void findServiceBy() throws Exception {
            for (ExpectedService expectedService : expectedServices) {
                RegisteredService registeredService = testFindServiceBy(expectedService.serviceIdExample);
                assertNotNull("findServiceBy(Service) did not return a registered service for id="
                              + expectedService.serviceIdExample,
                              registeredService);
                expectedService.assertEqualsService(registeredService);
            }
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} for a service that does not exist.
         */
        @Test
        public void findServiceByNegative() throws Exception {
            RegisteredService registeredService = testFindServiceBy("something");
            assertNull(
                    "findServiceBy(Service) unexpectedly returned registered service",
                    registeredService);
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(long)}.
         */
        @Test
        public void findServiceById() throws Exception {
            int expectedId = 1;
            // IDs start with "1", the "expectedServices" array is zero-based
            ExpectedService expectedService = expectedServices.get(expectedId - 1);

            RegisteredService registeredService = etcdServicesManger.findServiceBy(expectedId);

            assertNotNull("findServiceBy(long) did not return a service for id=" + expectedId, registeredService);
            expectedService.assertEqualsService(registeredService);
        }

        /**
         * Calls {@link EtcdServicesManager#findServiceBy(Service)} with a mocked service that returns
         * <code>serviceId</code> on {@link Service#getId()}.
         */
        private RegisteredService testFindServiceBy(String serviceId) {
            Service service = mock(Service.class);
            when(service.getId()).thenReturn(serviceId);
            return etcdServicesManger.findServiceBy(service);
        }

        /**
         * Calls {@link EtcdServicesManager#getAllServices()} and returns the {@link DoguChangeListener} passed to
         * {@link CloudoguRegistry#addDoguChangeListener(DoguChangeListener)}.
         */
        private DoguChangeListener assertGetAllServices() {
            Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
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

    /**
     * Tests for development mode.
     */
    public static class DevelopmentMode {
        /**
         * ID of the service used in development mode.
         */
        private static final long DEVELOPMENT_SERVICE_ID = 0;

        EtcdServicesManager etcdServicesManger =
                new EtcdServicesManager(null, "development", mock(CloudoguRegistry.class));

        /**
         * Test for {@link EtcdServicesManager#getAllServices()}.
         */
        @Test
        public void getAllServices() throws Exception {
            Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
            assertEquals("Unexpected amount of services returned in development mode", 1, allServices.size());
            assertEquals("Development service not returned ny getAllServices()", DEVELOPMENT_SERVICE_ID,
                                allServices.iterator().next().getId());
        }

        /**
         * Test for {@link EtcdServicesManager#matchesExistingService(Service)} for an HTTPS service.
         */
        @Test
        public void matchesExistingService() throws Exception {
            Service service = mock(Service.class);
            when(service.getId()).thenReturn("https://something");
            assertTrue("Unexpected value for https service", etcdServicesManger.matchesExistingService(service));
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} for services that are neither
         * IMAPS nor HTTPS.
         */
        @Test
        public void matchesExistingServiceNegative() throws Exception {
            Service service = mock(Service.class);
            when(service.getId()).thenReturn("something");
            assertFalse("Unexpected value for non-https service", etcdServicesManger.matchesExistingService(service));
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for an HTTPS service.
         */
        @Test
        public void findServiceByHttps() throws Exception {
            RegisteredService registeredService = testFindServiceBy("https://something");
            assertNotNull(
                    "findServiceBy(Service) did not return a registered service in development mode for https service",
                    registeredService);
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for an IMAPS service.
         */
        @Test
        public void findServiceByImaps() throws Exception {
            RegisteredService registeredService = testFindServiceBy("imaps://something");
            assertNotNull(
                    "findServiceBy(Service) did not return a registered service in development mode for imaps service",
                    registeredService);
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for services that are neither
         * IMAPS nor HTTPS.
         */
        @Test
        public void findServiceByNegative() throws Exception {
            RegisteredService registeredService = testFindServiceBy("something");
            assertNull(
                    "findServiceBy(Service) unexpectedly returned registered service in development mode for service that was neither https nor imaps",
                    registeredService);
        }

        /**
         * Test for {@link EtcdServicesManager#findServiceBy(long)} in development mode.
         */
        @Test
        public void findServiceById() throws Exception {
            RegisteredService service = etcdServicesManger.findServiceBy(DEVELOPMENT_SERVICE_ID);
            assertNotNull("findServiceBy(long) did not return a service in development mode", service);
        }

        /**
         * Calls {@link EtcdServicesManager#findServiceBy(Service)} with a mocked service that returns
         * <code>serviceId</code> on {@link Service#getId()}.
         */
        private RegisteredService testFindServiceBy(String serviceId) {
            Service service = mock(Service.class);
            when(service.getId()).thenReturn(serviceId);
            return etcdServicesManger.findServiceBy(service);
        }
    }
}