package de.triology.cas.oidc.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesServiceCreationException;
import junit.framework.TestCase;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegexRegisteredService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CesOidcServiceFactoryTest extends TestCase {

    /**
     * Test for {@link CesOIDCServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noAttributes() {
        // given
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
        CesServiceData serviceData = new CesServiceData("oidcClient", factory, null);

        try {
            // when
            factory.createNewService(1, null, null, serviceData);
            fail();
        } catch (CesServiceCreationException e) {
            // then
            assertNotNull(e);
            assertEquals("Cannot create service; Cannot find attributes", e.getMessage());
        }
    }

    /**
     * Test for {@link CesOIDCServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noClientID() {
        // given
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
        Map<String, String> attributes = new HashMap<>();
        CesServiceData serviceData = new CesServiceData("oidcClient", factory, attributes);

        try {
            // when
            factory.createNewService(1, null, null, serviceData);
            fail();
        } catch (CesServiceCreationException e) {
            // then
            assertNotNull(e);
            assertEquals("Cannot create service; Cannot find attribute: oauth_client_id", e.getMessage());
        }
    }

    /**
     * Test for {@link CesOIDCServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noClientSecret() {
        // given
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        CesServiceData serviceData = new CesServiceData("oidcClient", factory, attributes);

        try {
            // when
            factory.createNewService(1, null, null, serviceData);
            fail();
        } catch (CesServiceCreationException e) {
            // then
            assertNotNull(e);
            assertEquals("Cannot create service; Cannot find attribute: oauth_client_secret", e.getMessage());
        }
    }

    /**
     * Test for {@link CesOIDCServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_emptyLogoutURI() throws CesServiceCreationException {
        // given
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();

        String fqdn = "192.168.56.2";
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "superSecretHash");
        CesServiceData serviceData = new CesServiceData("oidcClient", factory, attributes);

        // when
        RegexRegisteredService service = factory.createNewService(1, fqdn, null, serviceData);

        // then
        verifyService(service);
        assertNull(service.getLogoutUrl());
    }

    /**
     * Test for {@link CesOIDCServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_givenLogoutURI() throws CesServiceCreationException {
        // given
        CesOIDCServiceFactory factory = new CesOIDCServiceFactory();

        String fqdn = "192.168.56.2";
        URI logoutUri = URI.create("/api/auth/oidc/logout");
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOIDCServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "superSecretHash");
        CesServiceData serviceData = new CesServiceData("oidcClient", factory, attributes);

        // when
        RegexRegisteredService service = factory.createNewService(1, fqdn, logoutUri, serviceData);

        // then
        verifyService(service);
        assertEquals("https://192.168.56.2/oidcClient/api/auth/oidc/logout", service.getLogoutUrl());
    }

    private void verifyService(RegexRegisteredService service) {
        assertEquals(1, service.getId());
        assertEquals("CesOIDCServiceFactory oidcClient", service.getName());
        assertEquals("https://((?i)192\\.168\\.56\\.2)(:443)?/oidcClient(/.*)?", service.getServiceId());

        assertTrue(service instanceof OidcRegisteredService);
        OidcRegisteredService oidcService = (OidcRegisteredService) service;
        assertEquals("[application/json, code]", oidcService.getSupportedResponseTypes().toString());
        assertEquals("[authorization_code]", oidcService.getSupportedGrantTypes().toString());
        assertEquals("superID", oidcService.getClientId());
        assertEquals("superSecretHash", oidcService.getClientSecret());
        assertTrue(oidcService.isBypassApprovalPrompt());
    }
}