package de.triology.cas.services.dogu;

import de.triology.cas.services.CesServiceData;
import junit.framework.TestCase;
import org.apereo.cas.services.CasRegisteredService;

import java.net.URI;
import java.net.URISyntaxException;

public class CesDoguServiceFactoryTest extends TestCase {

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex() {
        // given
        String fqdn = "local.CLOUDOGU.com";

        // when
        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertTrue("local.cloudogu.com".matches(fqdnRegex));
        assertTrue("LOCAL.CLOUDOGU.COM".matches(fqdnRegex));
        assertTrue("LoCaL.cLoUdOgU.cOm".matches(fqdnRegex));
        assertFalse("local.cloudoguAcom".matches(fqdnRegex));
        assertFalse("localBcloudoguAcom".matches(fqdnRegex));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_IP() {
        // given
        String fqdn = "192.168.56.2";

        // when
        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertTrue("192.168.56.2".matches(fqdnRegex));
        assertFalse("192A168.56.2".matches(fqdnRegex));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_withGivenLogoutProperty() throws URISyntaxException, CesServiceCreationException {
        // given
        CesDoguServiceFactory factory = new CesDoguServiceFactory();
        long id = 1;
        String fqdn = "192.168.56.2";
        String serviceName = "testService";
        URI logoutProperty = new URI("/api/mylogout");
        CesServiceData data = new CesServiceData(serviceName, factory);

        // when
        CasRegisteredService service = factory.createNewService(id, fqdn, logoutProperty, data);

        // then
        assertEquals(1, service.getId());
        assertEquals("CesDoguServiceFactory testService", service.getName());
        assertTrue("https://192.168.56.2/testService/test/WOW".matches(service.getServiceId()));
        assertTrue("https://192.168.56.2:443/testService/test/WOW".matches(service.getServiceId()));
        assertFalse("https://192.168.56.2:443/TESTService/test/WOW".matches(service.getServiceId()));
        assertEquals("https://192.168.56.2/testService/api/mylogout", service.getLogoutUrl());
    }

    /**
     * Test case for {@link CesDoguServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_withNoLogoutProperty() throws CesServiceCreationException {
        // given
        CesDoguServiceFactory factory = new CesDoguServiceFactory();
        long id = 1;
        String fqdn = "local.cloudogu.com";
        String serviceName = "testService";
        CesServiceData data = new CesServiceData(serviceName, factory);

        // when
        CasRegisteredService service = factory.createNewService(id, fqdn, null, data);

        // then
        assertEquals(1, service.getId());
        assertEquals("CesDoguServiceFactory testService", service.getName());
        assertTrue("https://local.cloudogu.com/testService/test/WOW".matches(service.getServiceId()));
        assertTrue("https://local.cloudogu.com:443/testService/test/WOW".matches(service.getServiceId()));
        assertFalse("https://local.cloudogu.com:443/TESTService/test/WOW".matches(service.getServiceId()));
        assertNull(service.getLogoutUrl());
    }
}