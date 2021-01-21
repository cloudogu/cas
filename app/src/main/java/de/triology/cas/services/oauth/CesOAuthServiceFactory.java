package de.triology.cas.services.oauth;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.ICesServiceFactory;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.support.oauth.services.OAuthCallbackAuthorizeService;
import org.jasig.cas.support.oauth.services.OAuthRegisteredService;

import java.net.URI;

/**
 * Creates all necessary services required to enable Oauth in CAS.
 */
public class CesOAuthServiceFactory implements ICesServiceFactory {

    public static final String SERVICE_OAUTH_CALLBACK_IDENTIFIER = "oauth_callback_service";
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_ID = "oauth_client_id";
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET = "oauth_client_secret";

    /**
     * Creates the necessary callback service for o auth. This service is required to redirect successful cas
     * logins and make them recognized as o auth verifications.
     *
     * @param id Internal ID of the service
     * @return a new OAuth callback authorisation service
     */
    public OAuthCallbackAuthorizeService createCallbackService(long id, String fqdn) {
        OAuthCallbackAuthorizeService service = new OAuthCallbackAuthorizeService();
        service.setId(id);
        service.setName(CesOAuthServiceFactory.class.getSimpleName() + " " + SERVICE_OAUTH_CALLBACK_IDENTIFIER);
        String serviceId = "https://" + fqdn + "(:443)?/" + "oauth2.0/callbackAuthorize";
        service.setServiceId(serviceId);
        service.setAllowedToProxy(true);
        return service;
    }

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
    private OAuthRegisteredService createOAuthClientService(long id, String name, String serviceID, String clientID, String clientSecret) {
        OAuthRegisteredService service = new OAuthRegisteredService();
        service.setId(id);
        service.setName(name);
        service.setServiceId(serviceID);
        service.setAllowedToProxy(true);
        service.setClientId(clientID);
        service.setClientSecret(clientSecret);
        return service;
    }


    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws Exception {
        // Get client id
        String clientID = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        if(clientID == null) {
            throw new Exception("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        }

        // Get client secret
        String clientSecret = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET);
        if(clientSecret == null) {
            throw new Exception("Cannot create OAuth client service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET);
        }

        String serviceId = "https://" + fqdn + "(:443)?/" + serviceData.getName() + "(/.*)?";
        return createOAuthClientService(id, serviceData.getName(), serviceId, clientID, clientSecret);
    }
}
