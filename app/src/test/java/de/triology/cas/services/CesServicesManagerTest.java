package de.triology.cas.services;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static de.triology.cas.services.CesServicesManager.STAGE_DEVELOPMENT;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesServicesManager}
 */
public class CesServicesManagerTest {
    /**
     * Logger of class under test.
     */
    private static final TestLogger LOG = TestLoggerFactory.getTestLogger(CesServiceManagerUnderTest.class);

    /**
     * Reset logger before each test.
     **/
    @Rule public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    /**
     * Rule for asserting exceptions.
     */
    @Rule public ExpectedException thrown = ExpectedException.none();

    CesServicesManagerStage servicesManagerStage = mock(CesServicesManagerStage.class);
    CesServicesManager etcdServicesManger = new CesServiceManagerUnderTest(null, "don't care", mock(Registry.class));

    /**
     * Test for {@link CesServicesManager#CesServiceManager(List, String, Registry)} for production.
     */
    @Test
    public void constructForProduction() throws Exception {
        new CesServicesManager(null, "something", null) {
            @Override
            protected CesServicesManagerStage createStage(String stageString, List<String> allowedAttributes,
                                                          Registry registry) {
                CesServicesManagerStage stage = super.createStage(stageString, allowedAttributes, registry);
                assertThat(stage, instanceOf(CesServicesManagerStageProductive.class));
                return stage;
            }
        };
    }

    /**
     * Test for {@link CesServicesManager#CesServiceManager(List, String, Registry)} for production.
     */
    @Test
    public void constructForDevelopment() throws Exception {
        new CesServicesManager(null, STAGE_DEVELOPMENT, null) {
            @Override
            protected CesServicesManagerStage createStage(String stageString, List<String> allowedAttributes,
                                                          Registry registry) {
                CesServicesManagerStage stage = super.createStage(stageString, allowedAttributes, registry);
                assertThat(stage, instanceOf(CesServicesManagerStageDevelopment.class));
                return stage;
            }
        };
    }

    /**
     * Test for {@link CesServicesManager#getAllServices()}.
     */
    @Test
    public void getAllServices() throws Exception {
        RegisteredService service1 = mock(RegisteredService.class);
        RegisteredService service2 = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, service1);
            put(23L, service2);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
        assertThat(allServices, containsInAnyOrder(service1, service2));
    }

    /**
     * Test for {@link CesServicesManager#getAllServices()} where the result is modified.
     */
    @Test
    public void assertGetAllServicesModify() {
        when(servicesManagerStage.getRegisteredServices()).thenReturn(new HashMap<>());
        thrown.expect(UnsupportedOperationException.class);

        Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
        allServices.add(mock(RegisteredService.class));
    }

    /**
     * Test for {@link CesServicesManager#matchesExistingService(Service)}.
     */
    @Test
    public void matchesExistingService() throws Exception {
        RegisteredService expectedRegisteredService = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, expectedRegisteredService);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        Service service = mock(Service.class);
        when(expectedRegisteredService.matches(service)).thenReturn(true);

        assertTrue("Unexpected value for matchesExistingService()", etcdServicesManger.matchesExistingService(service));
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Service)} for a services that does not exist.
     */
    @Test
    public void matchesExistingServiceNegative() throws Exception {
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, mock(RegisteredService.class));
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        assertFalse("Unexpected value for matchesExistingService()",
                    etcdServicesManger.matchesExistingService(mock(Service.class)));
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Service)}.
     */
    @Test
    public void findServiceBy() throws Exception {
        RegisteredService expectedRegisteredService = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, expectedRegisteredService);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        Service service = mock(Service.class);
        when(expectedRegisteredService.matches(service)).thenReturn(true);

        RegisteredService actualRegisteredService = etcdServicesManger.findServiceBy(service);
        assertEquals("findServiceBy(Service) did not return registered service", expectedRegisteredService,
                     actualRegisteredService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Service)} for a service that does not exist.
     */
    @Test
    public void findServiceByNegative() throws Exception {
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, mock(RegisteredService.class));
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        RegisteredService registeredService = etcdServicesManger.findServiceBy(mock(Service.class));
        assertNull("findServiceBy(Service) unexpectedly returned registered service", registeredService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(long)}.
     */
    @Test
    public void findServiceById() throws Exception {
        RegisteredService expectedService = mock(RegisteredService.class);
        RegisteredService serviceToClone = mock(RegisteredService.class);
        when(serviceToClone.clone()).thenReturn(expectedService);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, serviceToClone);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        RegisteredService actualService = etcdServicesManger.findServiceBy(23);
        assertEquals("findServiceBy(long) did not return registered service", expectedService, actualService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(long)} for a service that does not exist.
     */
    @Test
    public void findServiceByIdNegative() throws Exception {
        HashMap<Long, RegisteredService> expectedServices = new HashMap<Long, RegisteredService>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, mock(RegisteredService.class));
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        RegisteredService registeredService = etcdServicesManger.findServiceBy(42);
        assertNull("findServiceBy(long) unexpectedly returned registered service", registeredService);
    }

    /**
     * Test for {@link CesServicesManager#reload()}.
     */
    @Test
    public void reload() throws Exception {
        etcdServicesManger.reload();
        verify(etcdServicesManger.createStage(null,null,null)).updateRegisteredServices();
    }

    /**
     * Test for {@link CesServicesManager#save(RegisteredService)}.
     */
    @Test
    public void save() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("save");

        etcdServicesManger.save(mock(RegisteredService.class));
    }

    /**
     * Test for {@link CesServicesManager#delete(long)}.
     */
    @Test
    public void delete() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("delete");

        etcdServicesManger.delete(42L);
    }

    /**
     * Special {@link CesServicesManager} that return a mocked stage for unit testing in isolation.
     */
    class CesServiceManagerUnderTest extends CesServicesManager {
        public CesServiceManagerUnderTest(List<String> allowedAttributes, String stage, Registry registry) {
            super(allowedAttributes, stage, registry);
        }

        @Override
        protected CesServicesManagerStage createStage(String stageString, List<String> allowedAttributes,
                                                      Registry registry) {
            return servicesManagerStage;
        }
    }

}