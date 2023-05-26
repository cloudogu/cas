package de.triology.cas.oidc.services;

import de.triology.cas.services.CesServiceData;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import de.triology.cas.services.dogu.CesServiceCreationException;
import de.triology.cas.services.dogu.CesServiceFactory;
import org.apereo.cas.services.BaseWebBasedRegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Supplier;

/**
 * This factory is responsible to create and to configure new OAuth services.
 */
public class CesOAuthServiceFactory<T extends OAuthRegisteredService> implements CesServiceFactory {

    private final Supplier<T> supplier;

    protected static final Logger LOG = LoggerFactory.getLogger(CesOAuthServiceFactory.class);
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_ID = "oauth_client_id";
    public static final String ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH = "oauth_client_secret";

    public CesOAuthServiceFactory(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Creates a new empty service.
     *
     * @return the created service.
     */
    protected T createEmptyService() {
        return this.supplier.get();
    }

    /**
     * OAUTH in CAS require one client for each OAUTH application.
     * This method creates a new client with the given information.
     *
     * @param id               internal ID of the service
     * @param name             name of the service
     * @param serviceID        a regex that describes which requests should be accepted (e.g., '${server.prefix}/dogu'
     *                         only process request send over the named address)
     * @param clientID         public client id of the OAUTH application used for identification
     * @param clientSecretHash secret key from the OAUTH application used for authentication
     * @return a new client server for the given information of the OAUTH application
     */
    protected T createOAUTHClientService(long id, String logoutURI, String name, String serviceID, String clientID, String clientSecretHash) {
        var service = createEmptyService();
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
        LOG.debug("Created Service: N:{} - ID:{} - SecHash:{} - SID:{}", name, clientID, clientSecretObfuscated, serviceID);
        LOG.debug("Partition: {}", service.getSingleSignOnParticipationPolicy());
        return service;
    }

    @Override
    public BaseWebBasedRegisteredService createNewService(long id, String fqdn, URI casLogoutUri, CesServiceData serviceData) throws CesServiceCreationException {
        if (serviceData.getAttributes() == null) {
            throw new CesServiceCreationException("Cannot create service; Cannot find attributes");
        }

        // Get client id
        String clientID = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        if (clientID == null) {
            throw new CesServiceCreationException("Cannot create service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_ID);
        }

        // Get client secret
        String clientSecret = serviceData.getAttributes().get(ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH);
        if (clientSecret == null) {
            throw new CesServiceCreationException("Cannot create service; Cannot find attribute: " + ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH);
        }

        String serviceId = String.format("https://%s(:443)?/%s(/.*)?", CesDoguServiceFactory.generateServiceIdFqdnRegex(fqdn), serviceData.getName());
        if (casLogoutUri != null) {
            String logoutUri = String.format("https://%s/%s%s", fqdn, serviceData.getName(), casLogoutUri);
            return createOAUTHClientService(id, logoutUri, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        } else {
            return createOAUTHClientService(id, null, serviceData.getIdentifier(), serviceId, clientID, clientSecret);
        }
    }
}
