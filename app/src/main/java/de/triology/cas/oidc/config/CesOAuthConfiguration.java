package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.*;
import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.config.OidcConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.LogoutExecutionPlanConfigurer;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilderConfigurer;
import org.apereo.cas.logout.slo.SingleLogoutServiceMessageHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.validator.OAuth20ClientSecretValidator;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.support.oauth.web.views.OAuth20UserProfileViewRenderer;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.util.http.HttpClient;
import org.apereo.cas.web.UrlValidator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("CesOAuthConfiguration")
@AutoConfigureAfter(OidcConfiguration.class)
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class CesOAuthConfiguration {

    @Bean
    @RefreshScope
    public OAuth20UserProfileViewRenderer oauthUserProfileViewRenderer() {
        return new CesOAuthProfileRenderer();
    }

    @Bean
    @RefreshScope()
    public OAuth20ClientSecretValidator oauth20ClientSecretValidator(
            @Qualifier("oauthRegisteredServiceCipherExecutor")
            final CipherExecutor oauthRegisteredServiceCipherExecutor) {
        return new CesOAuthClientSecretValidator(oauthRegisteredServiceCipherExecutor);
    }

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oauthCasClientRedirectActionBuilder() {
        LOGGER.debug("Create CES-OAuth client redirect action builder...");
        return new CesOidcClientRedirectActionBuilder();
    }

    @Bean
    @RefreshScope
    public SingleLogoutMessageCreator oauthSingleLogoutMessageCreator() {
        return new CesOAuthSingleLogoutMessageCreator();
    }

    @Bean
    @RefreshScope
    public SingleLogoutServiceMessageHandler oauthSingleLogoutServiceMessageHandler(
            ObjectProvider<HttpClient> noRedirectHttpClient,
            CasConfigurationProperties casProperties,
            ObjectProvider<SingleLogoutServiceLogoutUrlBuilder> singleLogoutServiceLogoutUrlBuilder,
            ObjectProvider<AuthenticationServiceSelectionPlan> authenticationServiceSelectionPlan,
            ObjectProvider<ServicesManager> servicesManager) {
        return new CesOAuthSingleLogoutServiceMessageHandler(noRedirectHttpClient.getObject(),
                oauthSingleLogoutMessageCreator(),
                servicesManager.getObject(),
                singleLogoutServiceLogoutUrlBuilder.getObject(),
                casProperties.getSlo().isAsynchronous(),
                authenticationServiceSelectionPlan.getObject());
    }

    @Bean
    @RefreshScope
    public LogoutExecutionPlanConfigurer oauthLogoutExecutionPlanConfigurer(SingleLogoutServiceMessageHandler oauthSingleLogoutServiceMessageHandler) {
        return plan -> plan.registerSingleLogoutServiceMessageHandler(oauthSingleLogoutServiceMessageHandler);
    }

    @Bean(name = "defaultSingleLogoutServiceLogoutUrlBuilderConfigurer")
    public SingleLogoutServiceLogoutUrlBuilderConfigurer defaultSingleLogoutServiceLogoutUrlBuilderConfigurer(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            final UrlValidator urlValidator,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            final ServicesManager servicesManager) {
        return () -> new CesSingleLogoutServiceLogoutUrlBuilder(servicesManager, urlValidator);
    }
}
