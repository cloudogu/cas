package de.triology.cas.oauth.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.ICesServiceFactory;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredServiceLogoutType;
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
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET = "oauth_client_secret";

    /**
     * OAuth in CAS require one client for each OAuth application.
     * This method creates a new client with the given information.
     *
     * @param id           internal ID of the service
     * @param name         name of the service
     * @param serviceID    a regex that describes which requests should be accepted (e.g., '${server.prefix}/dogu'
     *                     only process request send over the named address)
     * @param clientID     public client id of the OAuth application used for identification
     * @param clientSecret secret key from the OAuth application used for authentication
     * @return a new client server for the given information of the OAuth application
     */
    private OAuthRegisteredService createOAuthClientService(long id, String logoutURI, String name, String serviceID, String clientID, String clientSecret) {
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
        service.setClientSecret(clientSecret);
        service.setBypassApprovalPrompt(true);
        logger.debug("Created OAuthService: N:{} - ID:{} - Sec:{} - SID:{}", name, clientID, clientSecret, serviceID);
        return service;
    }


    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws Exception {
        // Get client id
        String clientID = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        if (clientID == null) {
            throw new Exception("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        }

        // Get client secret
        String clientSecret = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET);
        if (clientSecret == null) {
            throw new Exception("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET);
        }

        String serviceId = "https://" + fqdn + "(:443)?/" + serviceData.getName() + "(/.*)?";
        if (casLogoutUri != null) {
            String logoutUri = "https://" + fqdn + "/" + serviceData.getName() + casLogoutUri;
            return createOAuthClientService(id, logoutUri, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        } else {
            return createOAuthClientService(id, null, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        }
    }
}
