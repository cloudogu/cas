package de.triology.cas.services;

import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.query.RegisteredServiceQuery;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static de.triology.cas.services.CesServicesManager.STAGE_DEVELOPMENT;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link CesServicesManager}
 */
public class CesServicesManagerTest {
    CesServicesManagerStage servicesManagerStage = mock(CesServicesManagerStage.class);
    CesServiceManagerConfiguration managerDevelopmentConfig = new CesServiceManagerConfiguration(STAGE_DEVELOPMENT, null);
    CesServiceManagerConfiguration managerConfig = new CesServiceManagerConfiguration("don't care", null);
    CesServicesManager etcdServicesManger = new CesServiceManagerUnderTest(managerConfig, mock(Registry.class));

    /**
     * Test for {@link CesServicesManager#CesServicesManager(CesServiceManagerConfiguration, Registry)} )} for production.
     */
    @Test
    public void constructForProduction() {
        new CesServicesManager(managerConfig, null) {
            @Override
            protected CesServicesManagerStage createStage(CesServiceManagerConfiguration managerConfig, Registry registry) {
                CesServicesManagerStage stage = super.createStage(managerConfig, registry);
                MatcherAssert.assertThat(stage, instanceOf(CesServicesManagerStageProductive.class));
                return stage;
            }
        };
    }

    /**
     * Test for {@link CesServicesManager#CesServicesManager(CesServiceManagerConfiguration, Registry)} for production.
     */
    @Test
    public void constructForDevelopment() {
        new CesServicesManager(managerDevelopmentConfig, null) {
            @Override
            protected CesServicesManagerStage createStage(CesServiceManagerConfiguration managerConfig, Registry registry) {
                CesServicesManagerStage stage = super.createStage(managerConfig, registry);
                MatcherAssert.assertThat(stage, instanceOf(CesServicesManagerStageDevelopment.class));
                return stage;
            }
        };
    }

    /**
     * Test for {@link CesServicesManager#getAllServices()}.
     */
    @Test
    public void getAllServices() {
        RegisteredService service1 = mock(RegisteredService.class);
        RegisteredService service2 = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, service1);
            put(23L, service2);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
        MatcherAssert.assertThat(allServices, containsInAnyOrder(service1, service2));
    }

    /**
     * Test for {@link CesServicesManager#getAllServices()}.
     */
    @Test
    public void getAllServicesOfType() {
        // given
        RegisteredService service1 = mock(RegisteredService.class);
        RegisteredService service2 = mock(RegisteredService.class);
        RegisteredService service3 = mock(OAuthRegisteredService.class);
        RegisteredService service4 = mock(OidcRegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, service1);
            put(23L, service2);
            put(50L, service3);
            put(51L, service4);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        // when
        Collection<RegisteredService> allRegisteredServices = etcdServicesManger.getAllServicesOfType(RegisteredService.class);
        // then
        MatcherAssert.assertThat(allRegisteredServices, containsInAnyOrder(service1, service2, service3, service4));
        assertEquals(allRegisteredServices.size(), 4);

        // when
        Collection<RegisteredService> allOauthServices = etcdServicesManger.getAllServicesOfType(OAuthRegisteredService.class);
        // then
        MatcherAssert.assertThat(allOauthServices, containsInAnyOrder(service3, service4));
        assertEquals(allOauthServices.size(), 2);

        // when
        Collection<RegisteredService> allOIDCServices = etcdServicesManger.getAllServicesOfType(OidcRegisteredService.class);
        // then
        MatcherAssert.assertThat(allOIDCServices, containsInAnyOrder(service4));
        assertEquals(allOIDCServices.size(), 1);

        // when
        Collection<RegisteredService> noServices = etcdServicesManger.getAllServicesOfType(CesServicesManager.class);
        // then
        assertEquals(noServices.size(), 0);
    }

    /**
     * Test for {@link CesServicesManager#getAllServices()} where the result is modified.
     */
    @Test
    public void assertGetAllServicesModify() {
        when(servicesManagerStage.getRegisteredServices()).thenReturn(new HashMap<>());
        assertThrows(UnsupportedOperationException.class, () -> {
            Collection<RegisteredService> allServices = etcdServicesManger.getAllServices();
            allServices.add(mock(RegisteredService.class));
        });
    }

    /**
     * Test for {@link CesServicesManager#getServicesForDomain(String)}.
     */
    @Test
    public void getServicesForDomain() {
        assertThrows(UnsupportedOperationException.class, () -> {
            etcdServicesManger.getServicesForDomain("someDomain");
        });
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Service)}.
     */
    @Test
    public void findServiceBy() {
        RegisteredService expectedRegisteredService = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
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
    public void findServiceByNegative() {
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, mock(RegisteredService.class));
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        RegisteredService registeredService = etcdServicesManger.findServiceBy(mock(Service.class));
        assertNull("findServiceBy(Service) unexpectedly returned registered service", registeredService);
    }

    /**
     * Test for {@link CesServicesManager#findServicesBy(RegisteredServiceQuery[])}.
     */
    @Test
    public void findServiceByQuery() {
        OAuthRegisteredService expectedRegisteredService = mock(OAuthRegisteredService.class);
        when(expectedRegisteredService.getClientId()).thenReturn("portainer");

        OAuthRegisteredService otherRegisteredService = mock(OAuthRegisteredService.class);
        when(otherRegisteredService.getClientId()).thenReturn("otherService");

        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, otherRegisteredService);
            put(4L, mock(RegisteredService.class));
            put(23L, expectedRegisteredService);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        RegisteredServiceQuery query = mock(RegisteredServiceQuery.class);
        when(query.getType()).thenReturn(OAuthRegisteredService.class);
        when(query.getName()).thenReturn("clientId");
        when(query.getValue()).thenReturn("portainer");
        when(query.isIncludeAssignableTypes()).thenReturn(true);

        RegisteredServiceQuery query2 = mock(RegisteredServiceQuery.class);
        when(query2.getName()).thenReturn("notSupported");

        List<RegisteredService> actualRegisteredServices = etcdServicesManger.findServicesBy(query, query2).toList();
        assertEquals(1, actualRegisteredServices.size());
        assertEquals("findServicesBy(Query) did not return registered service", expectedRegisteredService,
                actualRegisteredServices.getFirst());
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(long)}.
     */
    @Test
    public void findServiceById() {
        RegisteredService expectedService = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, expectedService);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        RegisteredService actualService = etcdServicesManger.findServiceBy(23);
        assertEquals("findServiceBy(long) did not return registered service", expectedService, actualService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(long)} for a service that does not exist.
     */
    @Test
    public void findServiceByIdNegative() {
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, mock(RegisteredService.class));
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);
        RegisteredService registeredService = etcdServicesManger.findServiceBy(42);
        assertNull("findServiceBy(long) unexpectedly returned registered service", registeredService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Predicate)}.
     */
    @Test
    public void findServiceByPredicate() {
        assertThrows(UnsupportedOperationException.class, () -> {
            etcdServicesManger.findServiceBy(registeredService -> false);
        });
    }

    /**
     * Test for {@link CesServicesManager#findServiceBy(Service, Class)} .
     */
    @Test
    public void findServiceByServiceAndClass() {
        RegisteredService expectedRegisteredService = mock(RegisteredService.class);
        HashMap<Long, RegisteredService> expectedServices = new HashMap<>() {{
            put(0L, mock(RegisteredService.class));
            put(23L, expectedRegisteredService);
        }};
        when(servicesManagerStage.getRegisteredServices()).thenReturn(expectedServices);

        Service service = mock(Service.class);
        when(expectedRegisteredService.matches(service)).thenReturn(true);

        RegisteredService actualRegisteredService = etcdServicesManger.findServiceBy(service, RegisteredService.class);
        assertEquals("findServiceBy(Service, Class) did not return registered service", expectedRegisteredService,
                actualRegisteredService);


        actualRegisteredService = etcdServicesManger.findServiceBy(null, RegisteredService.class);
        assertNull("findServiceBy(Service, Class) did not return null",  actualRegisteredService);

        actualRegisteredService = etcdServicesManger.findServiceBy(service, OAuthRegisteredService.class);
        assertNull("findServiceBy(Service, Class) did not return null",  actualRegisteredService);
    }

    /**
     * Test for {@link CesServicesManager#findServiceByName(String)}
     */
    @Test
    public void findServiceByName() {
        assertThrows(UnsupportedOperationException.class, () -> {
            etcdServicesManger.findServiceByName("name");
        });
    }

    /**
     * Test for {@link CesServicesManager#load()}.
     */
    @Test
    public void load() {
        etcdServicesManger.load();
        verify(etcdServicesManger.createStage(managerConfig, null)).updateRegisteredServices();
    }

    /**
     * Test for {@link CesServicesManager#save(RegisteredService)}.
     */
    @Test
    public void save() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.save(mock(RegisteredService.class)));
    }

    /**
     * Test for {@link CesServicesManager#save(Stream)} .
     */
    @Test
    public void save1() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.save(Stream.empty()));
    }

    /**
     * Test for {@link CesServicesManager#save(RegisteredService, boolean)}  .
     */
    @Test
    public void save3() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.save(mock(RegisteredService.class), true));
    }

    /**
     * Test for {@link CesServicesManager#save(Supplier, Consumer, long)}  .
     */
    @Test
    public void save4() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.save(mock(Supplier.class), mock(Consumer.class), 3));
    }

    /**
     * Test for {@link CesServicesManager#deleteAll()}   .
     */
    @Test
    public void deleteAll() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.deleteAll());
    }

    /**
     * Test for {@link CesServicesManager#delete(long)}.
     */
    @Test
    public void delete() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.delete(42L));
    }

    /**
     * Test for {@link CesServicesManager#delete(RegisteredService)}   .
     */
    @Test
    public void delete2() {
        assertThrows(UnsupportedOperationException.class, () -> etcdServicesManger.delete(mock(RegisteredService.class)));
    }

    /**
     * Special {@link CesServicesManager} that return a mocked stage for unit testing in isolation.
     */
    class CesServiceManagerUnderTest extends CesServicesManager {
        public CesServiceManagerUnderTest(CesServiceManagerConfiguration managerConfig, Registry registry) {
            super(managerConfig, registry);
        }

        @Override
        protected CesServicesManagerStage createStage(CesServiceManagerConfiguration managerConfig, Registry registry) {
            return servicesManagerStage;
        }
    }

}