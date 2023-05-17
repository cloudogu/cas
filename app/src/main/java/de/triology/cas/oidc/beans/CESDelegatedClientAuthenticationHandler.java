package de.triology.cas.oidc.beans;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.principal.ClientCredential;
import org.apereo.cas.authentication.principal.Principal;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.provision.DelegatedClientUserProfileProvisioner;
import org.apereo.cas.integration.pac4j.authentication.handler.support.AbstractPac4jAuthenticationHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.pac4j.authentication.handler.support.DelegatedClientAuthenticationHandler;
import org.apereo.cas.web.support.WebUtils;
import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.UserProfile;

import java.util.Objects;
import java.util.Optional;

/**
 * Pac4j authentication handler which gets the credentials and then the user profile
 * in a delegated authentication process from an external identity provider.
 *
 * @author Jerome Leleu
 * @since 3.5.0
 */
@Slf4j
public class CESDelegatedClientAuthenticationHandler extends DelegatedClientAuthenticationHandler {


    private final Clients clients;
    private final DelegatedClientUserProfileProvisioner profileProvisioner;
    public CESDelegatedClientAuthenticationHandler(final String name,
                                                final Integer order,
                                                final ServicesManager servicesManager,
                                                final PrincipalFactory principalFactory,
                                                final Clients clients,
                                                final DelegatedClientUserProfileProvisioner profileProvisioner,
                                                final SessionStore sessionStore) {
        super(name, order, servicesManager, principalFactory, clients, profileProvisioner, sessionStore);
        this.clients = clients;
        this.profileProvisioner = profileProvisioner;
    }
    @Override
    protected AuthenticationHandlerExecutionResult doAuthentication(final Credential credential) throws PreventedException {
        try {

            val clientCredentials = (ClientCredential) credential;
            log.debug("CESWAY: Located client credentials as [{}]", clientCredentials);

            log.trace("CESWAY: Client name: [{}]", clientCredentials.getClientName());

            val clientResult = clients.findClient(clientCredentials.getClientName());
            if (clientResult.isEmpty()) {
                throw new IllegalArgumentException("Unable to determine client based on client name " + clientCredentials.getClientName());
            }
            val client = BaseClient.class.cast(clientResult.get());
            log.trace("CESWAY: Delegated client is: [{}]", client);

            val request = WebUtils.getHttpServletRequestFromExternalWebflowContext();
            val response = WebUtils.getHttpServletResponseFromExternalWebflowContext();
            val webContext = new JEEContext(Objects.requireNonNull(request),
                    Objects.requireNonNull(response));

            var userProfileResult = Optional.ofNullable(clientCredentials.getUserProfile());
            if (userProfileResult.isEmpty()) {
                val credentials = clientCredentials.getCredentials();
                userProfileResult = client.getUserProfile(credentials, webContext, this.sessionStore);
            }
            if (userProfileResult.isEmpty()) {
                throw new PreventedException("Unable to fetch user profile from client " + client.getName());
            }
            val userProfile = userProfileResult.get();

            if (this.userExistsInLdap(userProfile)){
                this.updateUserInLdap(userProfile);

                storeUserProfile(webContext, userProfile);
                return createResult(clientCredentials, userProfile, client);
            } else {
                throw new RuntimeException("This user is not allowed to login here");
            }

        } catch (final Exception e) {
            throw new PreventedException(e);
        }
    }

    private boolean isOidcClient(BaseClient client){
        return client.getName().equals("keycloak");
    }

    private boolean userExistsInLdap(UserProfile profile){
        return profile.getAttribute("preferred_username").equals("ecosystem");
    }

    private void updateUserInLdap(UserProfile profile){

    }
}
