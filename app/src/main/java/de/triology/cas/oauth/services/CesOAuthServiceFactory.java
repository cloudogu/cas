package de.triology.cas.oauth.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesServiceCreationException;
import de.triology.cas.services.dogu.ICesServiceFactory;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Creates all necessary services required to enable Oauth in CAS.
 */
public class CesOAuthServiceFactory implements ICesServiceFactory {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_ID = "oauth_client_id";
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH = "oauth_client_secret";
    public static final String SERVICE_OIDC_IDENTIFIER = "testOIDCClient";

    /**
     * OAuth in CAS require one client for each OAuth application.
     * This method creates a new client with the given information.
     *
     * @param id               internal ID of the service
     * @param name             name of the service
     * @param serviceID        a regex that describes which requests should be accepted (e.g., '${server.prefix}/dogu'
     *                         only process request send over the named address)
     * @param clientID         public client id of the OAuth application used for identification
     * @param clientSecretHash secret key from the OAuth application used for authentication
     * @return a new client server for the given information of the OAuth application
     */
    private OAuthRegisteredService createOAuthClientService(long id, String logoutURI, String name, String serviceID, String clientID, String clientSecretHash) {
        OAuthRegisteredService service = new OAuthRegisteredService();
        service.setId(id);
        service.setName(name);
        service.setServiceId(serviceID);
        if (logoutURI != null) {
            service.setLogoutUrl(logoutURI);
        }
        service.getSupportedResponseTypes().add("application/json");
        service.getSupportedResponseTypes().add("code");
        service.getSupportedGrantTypes().add("authorization_code");
        service.setClientId(clientID);
        service.setClientSecret(clientSecretHash);
        service.setBypassApprovalPrompt(true);

        String clientSecretObfuscated = clientSecretHash.substring(0, 5) + "****" + clientSecretHash.substring(clientSecretHash.length() - 5);
        logger.debug("Created OAuthService: N:{} - ID:{} - SecHash:{} - SID:{}", name, clientID, clientSecretObfuscated, serviceID);
        return service;
    }


    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws CesServiceCreationException {
        // Get client id
        String clientID = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        if (clientID == null) {
            throw new CesServiceCreationException("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        }

        // Get client secret
        String clientSecret = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH);
        if (clientSecret == null) {
            throw new CesServiceCreationException("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH);
        }

        String serviceId = "https://" + fqdn + "(:443)?/" + serviceData.getName() + "(/.*)?";
        if (casLogoutUri != null) {
            String logoutUri = "https://" + fqdn + "/" + serviceData.getName() + casLogoutUri;
            return createOAuthClientService(id, logoutUri, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        } else {
            return createOAuthClientService(id, null, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        }
    }

    public OidcRegisteredService createOIDCService(long id, String fqdn, String logoutURI) {
        OidcRegisteredService service = new OidcRegisteredService();
        service.setId(id);
        service.setName(CesOAuthServiceFactory.class.getSimpleName() + " " + SERVICE_OIDC_IDENTIFIER);
        service.setServiceId(".*");

        String logoutUrl = "http://localhost:8080/api/oauth/oidc/logout";
        service.setLogoutUrl(logoutUrl);
        service.getSupportedResponseTypes().add("application/json");
        service.getSupportedResponseTypes().add("code");
        service.getSupportedGrantTypes().add("authorization_code");
        service.setClientId(SERVICE_OIDC_IDENTIFIER);
        String clientSecretHash = org.apache.commons.codec.digest.DigestUtils.sha256Hex("clientSecret");
        service.setClientSecret(clientSecretHash);
        service.setBypassApprovalPrompt(true);

        logger.debug("Created OidcRegisteredService: N:{} - ID:{} - SecHash:{} - SID:{}", service.getName(), "testClient", clientSecretHash, ".*");
        return service;
    }
}
