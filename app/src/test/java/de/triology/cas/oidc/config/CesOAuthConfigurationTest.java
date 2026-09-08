package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.CesOAuthClientSecretValidator;
import de.triology.cas.oidc.beans.CesOAuthProfileRenderer;
import de.triology.cas.oidc.beans.CesOAuthSingleLogoutMessageCreator;
import de.triology.cas.oidc.beans.CesOAuthSingleLogoutServiceMessageHandler;
import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.CesSingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.slo.SingleLogoutProperties;
import org.apereo.cas.logout.LogoutExecutionPlan;
import org.apereo.cas.logout.LogoutExecutionPlanConfigurer;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilderConfigurer;
import org.apereo.cas.logout.slo.SingleLogoutServiceMessageHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.validator.OAuth20ClientSecretValidator;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.web.UrlValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CesOAuthConfigurationTest {

    private final CesOAuthConfiguration config = new CesOAuthConfiguration();

    @Test
    void oauthUserProfileViewRenderer_returnsCesRenderer() {
        assertInstanceOf(CesOAuthProfileRenderer.class, config.oauthUserProfileViewRenderer());
    }

    @Test
    void oauth20ClientSecretValidator_returnsCesValidator() {
        @SuppressWarnings("unchecked")
        CipherExecutor<Serializable, String> cipherExecutor = mock(CipherExecutor.class);

        OAuth20ClientSecretValidator validator = config.oauth20ClientSecretValidator(cipherExecutor);

        assertInstanceOf(CesOAuthClientSecretValidator.class, validator);
    }

    @Test
    void oauthCasClientRedirectActionBuilder_returnsCesBuilder() {
        OAuth20CasClientRedirectActionBuilder builder = config.oauthCasClientRedirectActionBuilder();

        assertInstanceOf(CesOidcClientRedirectActionBuilder.class, builder);
    }

    @Test
    void oauthSingleLogoutMessageCreator_returnsCesMessageCreator() {
        TicketRegistry ticketRegistry = mock(TicketRegistry.class);

        var creator = config.oauthSingleLogoutMessageCreator(ticketRegistry);

        assertInstanceOf(CesOAuthSingleLogoutMessageCreator.class, creator);
    }

    @Test
    void oauthSingleLogoutServiceMessageHandler_returnsCesHandler() {
        @SuppressWarnings("unchecked")
        ObjectProvider<HttpClient> noRedirectHttpClient = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<SingleLogoutServiceLogoutUrlBuilder> logoutUrlBuilderProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<AuthenticationServiceSelectionPlan> authnServiceSelectionPlanProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ServicesManager> servicesManagerProvider = mock(ObjectProvider.class);

        when(noRedirectHttpClient.getObject()).thenReturn(mock(HttpClient.class));
        when(logoutUrlBuilderProvider.getObject()).thenReturn(mock(SingleLogoutServiceLogoutUrlBuilder.class));
        when(authnServiceSelectionPlanProvider.getObject()).thenReturn(mock(AuthenticationServiceSelectionPlan.class));
        when(servicesManagerProvider.getObject()).thenReturn(mock(ServicesManager.class));

        CasConfigurationProperties casProperties = mock(CasConfigurationProperties.class);
        SingleLogoutProperties sloProperties = mock(SingleLogoutProperties.class);
        when(casProperties.getSlo()).thenReturn(sloProperties);
        when(sloProperties.isAsynchronous()).thenReturn(true);

        TicketRegistry ticketRegistry = mock(TicketRegistry.class);

        SingleLogoutServiceMessageHandler handler = config.oauthSingleLogoutServiceMessageHandler(
                noRedirectHttpClient, casProperties, logoutUrlBuilderProvider, authnServiceSelectionPlanProvider,
                servicesManagerProvider, ticketRegistry);

        assertInstanceOf(CesOAuthSingleLogoutServiceMessageHandler.class, handler);
    }

    @Test
    void cesOAuthLogoutExecutionPlanConfigurer_registersHandlerOnPlan() {
        SingleLogoutServiceMessageHandler handler = mock(SingleLogoutServiceMessageHandler.class);

        LogoutExecutionPlanConfigurer configurer = config.cesOAuthLogoutExecutionPlanConfigurer(handler);
        assertNotNull(configurer);

        LogoutExecutionPlan plan = mock(LogoutExecutionPlan.class);
        configurer.configureLogoutExecutionPlan(plan);

        verify(plan).registerSingleLogoutServiceMessageHandler(handler);
    }

    @Test
    void defaultSingleLogoutServiceLogoutUrlBuilderConfigurer_buildsCesUrlBuilder() {
        UrlValidator urlValidator = mock(UrlValidator.class);
        ServicesManager servicesManager = mock(ServicesManager.class);

        SingleLogoutServiceLogoutUrlBuilderConfigurer configurer =
                config.defaultSingleLogoutServiceLogoutUrlBuilderConfigurer(urlValidator, servicesManager);
        assertNotNull(configurer);

        SingleLogoutServiceLogoutUrlBuilder builder = configurer.configureBuilder();

        assertInstanceOf(CesSingleLogoutServiceLogoutUrlBuilder.class, builder);
    }
}
