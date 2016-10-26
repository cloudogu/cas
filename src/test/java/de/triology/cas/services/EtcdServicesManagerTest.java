package de.triology.cas.services;

import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;
import uk.org.lidalia.slf4jtest.TestLoggerFactoryResetRule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.org.lidalia.slf4jtest.LoggingEvent.info;

/**
 * Tests for {@link EtcdServicesManager}
 */
public class EtcdServicesManagerTest {

    /**
     * ID of the service used in development mode.
     */
    private static final long DEVELOPMENT_SERVICE_ID = 0;

    private final List<String> allowedAttributes = new LinkedList<>();

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

    @Before
    public void setUp() throws Exception {

    }

    /**
     * Test for {@link EtcdServicesManager#getAllServices()} in development mode.
     */
    @Test
    public void getAllServicesDevelopment() throws Exception {
        EtcdServicesManager etc = createEtcdServicesManger("development");
        Collection<RegisteredService> allServices = etc.getAllServices();
        assertEquals("Unexpected amount of services returned in development mode", 1, allServices.size());
        assertEquals("Development service not returned ny getAllServices()", DEVELOPMENT_SERVICE_ID,
                     allServices.iterator().next().getId());
    }

    /**
     * Test for {@link EtcdServicesManager#matchesExistingService(Service)} in development mode for an HTTPS service.
     */
    @Test
    public void matchesExistingService() throws Exception {
        EtcdServicesManager etc = createEtcdServicesManger("development");
        Service service = mock(Service.class);
        when(service.getId()).thenReturn("https://something");
        assertTrue("Unexpected value for https service", etc.matchesExistingService(service));
    }


    /**
     * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for services that are neither
     * IMAPS nor HTTPS.
     */
    @Test
    public void matchesExistingServiceNegative() throws Exception {
        EtcdServicesManager etc = createEtcdServicesManger("development");
        Service service = mock(Service.class);
        when(service.getId()).thenReturn("something");
        assertFalse("Unexpected value for non-https service", etc.matchesExistingService(service));
    }

    /**
     * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for an HTTPS service.
     */
    @Test
    public void findServiceByDevelopmentHttps() throws Exception {
        RegisteredService registeredService = testFindServiceByDevelopment("https://something");
        assertNotNull(
                "findServiceBy(Service) did not return a registered service in development mode for https service",
                registeredService);
    }

    /**
     * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for an IMAPS service.
     */
    @Test
    public void findServiceByDevelopmentImaps() throws Exception {
        RegisteredService registeredService = testFindServiceByDevelopment("imaps://something");
        assertNotNull(
                "findServiceBy(Service) did not return a registered service in development mode for imaps service",
                registeredService);
    }

    /**
     * Test for {@link EtcdServicesManager#findServiceBy(Service)} in development mode for services that are neither
     * IMAPS nor HTTPS.
     */
    @Test
    public void findServiceByDevelopmentNegative() throws Exception {
        RegisteredService registeredService = testFindServiceByDevelopment("something");
        assertNull(
                "findServiceBy(Service) unexpectedly returned registered service in development mode for service that was neither https nor imaps",
                registeredService);
    }

    /**
     * Test for {@link EtcdServicesManager#findServiceBy(long)} in development mode.
     */
    @Test
    public void findServiceByIdDevelopment() throws Exception {
        EtcdServicesManager etc = createEtcdServicesManger("development");
        RegisteredService service = etc.findServiceBy(DEVELOPMENT_SERVICE_ID);
        assertNotNull("findServiceBy(long) did not return a service in development mode", service);
    }

    /**
     * Test for {@link EtcdServicesManager#reload()}.
     */
    @Test
    public void reload() throws Exception {
        EtcdServicesManager etc = createEtcdServicesManger("don't care");
        etc.reload();

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

        EtcdServicesManager etc = createEtcdServicesManger("don't care");
        etc.save(mock(RegisteredService.class));
    }

    /**
     * Test for {@link EtcdServicesManager#delete(long)}.
     */
    @Test
    public void delete() throws Exception {
        thrown.expect(UnsupportedOperationException.class);
        thrown.expectMessage("delete");

        EtcdServicesManager etc = createEtcdServicesManger("don't care");
        etc.delete(42L);
    }

    /**
     * Calls {@link EtcdServicesManager#findServiceBy(Service)} with a mocked service that returns
     * <code>serviceId</code> on {@link Service#getId()}.
     */
    private RegisteredService testFindServiceByDevelopment(String serviceId) {
        EtcdServicesManager etc = createEtcdServicesManger("development");
        Service service = mock(Service.class);
        when(service.getId()).thenReturn(serviceId);
        return etc.findServiceBy(service);
    }

    private EtcdServicesManager createEtcdServicesManger(String stage) {
        return new EtcdServicesManager(allowedAttributes, stage);
    }

}