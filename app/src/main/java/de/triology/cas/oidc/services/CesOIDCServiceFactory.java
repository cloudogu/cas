package de.triology.cas.oidc.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import de.triology.cas.services.dogu.ICesServiceFactory;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.services.RegexRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Creates all necessary services required to enable OIDC in CAS
 */
public class CesOIDCServiceFactory implements ICesServiceFactory {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    public static final String ATTRIBUTE_KEY_OIDC_CLIENT_ID = "oidc_client_id";
    public static final String ATTRIBUTE_KEY_OIDC_CLIENT_SECRET_HASH = "oidc_client_secret";

    /**
     * OIDC in CAS require one client for each OIDC application.
     * This method creates a new client with the given information.
     *
     * @param id               internal ID of the service
     * @param name             name of the service
     * @param serviceID        a regex that describes which requests should be accepted (e.g., '${server.prefix}/dogu'
     *                         only process request send over the named address)
     * @param clientID         public client id of the OIDC application used for identification
     * @param clientSecretHash secret key from the OIDC application used for authentication
     * @return a new client server for the given information of the OIDC application
     */
    private OidcRegisteredService createOIDCClientService(long id, String logoutURI, String name, String serviceID, String clientID, String clientSecretHash) {
        OidcRegisteredService service = new OidcRegisteredService();
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
        logger.debug("Created OidcRegisteredService: N:{} - ID:{} - SecHash:{} - SID:{}", name, clientID, clientSecretObfuscated, serviceID);
        return service;
    }

    @Override
    public RegexRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws CesServiceCreationException {
        if (serviceData.getAttributes() == null) {
            throw new CesServiceCreationException("Cannot create OIDC client service; Cannot find attributes");
        }

        // Get client id
        String clientID = serviceData.getAttributes().get(ATTRIBUTE_KEY_OIDC_CLIENT_ID);
        if (clientID == null) {
            throw new CesServiceCreationException("Cannot create OIDC client service; Cannot find attribute: " + ATTRIBUTE_KEY_OIDC_CLIENT_ID);
        }

        // Get client secret
        String clientSecret = serviceData.getAttributes().get(ATTRIBUTE_KEY_OIDC_CLIENT_SECRET_HASH);
        if (clientSecret == null) {
            throw new CesServiceCreationException("Cannot create OIDC client service; Cannot find attribute: " + ATTRIBUTE_KEY_OIDC_CLIENT_SECRET_HASH);
        }

        String serviceId = "https://" + CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn) + "(:443)?/" + serviceData.getName() + "(/.*)?";
        if (casLogoutUri != null) {
            String logoutUri = "https://" + fqdn + "/" + serviceData.getName() + casLogoutUri;
            return createOIDCClientService(id, logoutUri, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        } else {
            return createOIDCClientService(id, null, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        }
    }
}
