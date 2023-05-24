package de.triology.cas.oidc.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesServiceCreationException;
import junit.framework.TestCase;
import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CesOAuthServiceFactoryTest extends TestCase {

    protected void verifyService(RegisteredService service) {
        assertEquals(1, service.getId());
        assertEquals("CesOAuthServiceFactory clientName", service.getName());
        assertEquals("https://((?i)192\\.168\\.56\\.2)(:443)?/clientName(/.*)?", service.getServiceId());

        assertTrue(service instanceof OAuthRegisteredService);
        OAuthRegisteredService oidcService = (OAuthRegisteredService) service;
        assertEquals("[application/json, code]", oidcService.getSupportedResponseTypes().toString());
        assertEquals("[authorization_code]", oidcService.getSupportedGrantTypes().toString());
        assertEquals("superID", oidcService.getClientId());
        assertEquals("superSecretHash", oidcService.getClientSecret());
        assertTrue(oidcService.isBypassApprovalPrompt());
    }

    /**
     * Test for {@link CesOAuthServiceFactory#createEmptyService()}
     */
    public void testCreateEmptyService() {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();

        // when
        OAuthRegisteredService service = factory.createEmptyService();

        // then
        assertTrue(service != null);
    }

    /**
     * Test for {@link CesOAuthServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noAttributes() {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();
        CesServiceData serviceData = new CesServiceData("clientName", factory, null);

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
     * Test for {@link CesOAuthServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noClientID() {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();
        Map<String, String> attributes = new HashMap<>();
        CesServiceData serviceData = new CesServiceData("clientName", factory, attributes);

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
     * Test for {@link CesOAuthServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_noClientSecret() {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        CesServiceData serviceData = new CesServiceData("clientName", factory, attributes);

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
     * Test for {@link CesOAuthServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_emptyLogoutURI() throws CesServiceCreationException {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();

        String fqdn = "192.168.56.2";
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "superSecretHash");
        CesServiceData serviceData = new CesServiceData("clientName", factory, attributes);

        // when
        OAuthRegisteredService service = (OAuthRegisteredService)factory.createNewService(1, fqdn, null, serviceData);

        // then
        verifyService(service);
        assertNull(service.getLogoutUrl());
    }

    /**
     * Test for {@link CesOAuthServiceFactory#createNewService(long, String, URI, CesServiceData)}
     */
    public void testCreateNewService_givenLogoutURI() throws CesServiceCreationException {
        // given
        CesOAuthServiceFactory factory = new CesOAuthServiceFactory();

        String fqdn = "192.168.56.2";
        URI logoutUri = URI.create("/api/auth/oidc/logout");
        Map<String, String> attributes = new HashMap<>();
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID, "superID");
        attributes.put(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH, "superSecretHash");
        CesServiceData serviceData = new CesServiceData("clientName", factory, attributes);

        // when
        OAuthRegisteredService service = (OAuthRegisteredService)factory.createNewService(1, fqdn, logoutUri, serviceData);

        // then
        verifyService(service);
        assertEquals("https://192.168.56.2/clientName/api/auth/oidc/logout", service.getLogoutUrl());
    }
}