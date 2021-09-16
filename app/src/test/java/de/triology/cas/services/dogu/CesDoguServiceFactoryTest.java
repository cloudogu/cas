package de.triology.cas.services.dogu;

import de.triology.cas.services.CesServiceData;
import junit.framework.TestCase;
import org.apereo.cas.services.RegexRegisteredService;

import java.net.URI;
import java.net.URISyntaxException;

public class CesDoguServiceFactoryTest extends TestCase {

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_AllLower() {
        // given
        String fqdn = "local.cloudogu.com";
        String expected = "(?i)local.cloudogu.com";

        // when
        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_AllUpper() {
        // given
        String fqdn = "SUPER.cloudogu.com";
        String expected = "(?i)SUPER.cloudogu.com";

        // when
        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_Mixed() {
        // given
        String fqdn = "SuPer.loCAl.cloUDogu.cOM";
        String expected = "(?i)SuPer.loCAl.cloUDogu.cOM";

        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
    }

    /**
     * Test case for {@link CesDoguServiceFactory#generateServiceIdFqdnRegex(String)}
     */
    public void testGenerateServiceIdFqdnRegex_IP() {
        // given
        String fqdn = "192.168.56.2";
        String expected = "(?i)192.168.56.2";

        // when
        String fqdnRegex = CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn);

        // then
        assertEquals(expected, fqdnRegex);
        assertTrue(fqdn.matches(expected));
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
        String expectedServiceName = "CesDoguServiceFactory testService";
        String expectedServiceID = "https://(?i)192.168.56.2(:443)?/testService(/.*)?";
        String expectedLogoutUri = "https://192.168.56.2/testService/api/mylogout";

        // when
        RegexRegisteredService service = factory.createNewService(id, fqdn, logoutProperty, data);

        // then
        assertEquals(1, service.getId());
        assertEquals(expectedServiceName, service.getName());
        assertEquals(expectedServiceID, service.getServiceId());
        assertEquals(expectedLogoutUri, service.getLogoutUrl());
    }

    /**
     * Test case for {@link CesDoguServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_withNoLogoutProperty() throws CesServiceCreationException {
        // given
        CesDoguServiceFactory factory = new CesDoguServiceFactory();
        long id = 1;
        String fqdn = "192.168.56.2";
        String serviceName = "testService";
        CesServiceData data = new CesServiceData(serviceName, factory);
        String expectedServiceName = "CesDoguServiceFactory testService";
        String expectedServiceID = "https://(?i)192.168.56.2(:443)?/testService(/.*)?";

        // when
        RegexRegisteredService service = factory.createNewService(id, fqdn, null, data);

        // then
        assertEquals(1, service.getId());
        assertEquals(expectedServiceName, service.getName());
        assertEquals(expectedServiceID, service.getServiceId());
        assertNull(service.getLogoutUrl());
    }
}