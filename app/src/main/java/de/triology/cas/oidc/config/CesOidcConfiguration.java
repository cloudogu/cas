package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import de.triology.cas.oidc.beans.CesOidcSingleLogoutServiceMessageHandler;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.logout.slo.SingleLogoutMessageCreator;
import org.apereo.cas.logout.slo.SingleLogoutServiceLogoutUrlBuilder;
import org.apereo.cas.logout.slo.SingleLogoutServiceMessageHandler;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.apereo.cas.oidc.slo.OidcSingleLogoutServiceMessageHandler;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.apereo.cas.util.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("CesOidcConfiguration")
@ComponentScan("de.triology.cas.oidc")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@AutoConfigureAfter(OidcConfiguration.class)
public class CesOidcConfiguration {
    protected static final Logger LOG = LoggerFactory.getLogger(CesOidcConfiguration.class);

    @Bean
    @RefreshScope
    public OAuth20CasClientRedirectActionBuilder oidcCasClientRedirectActionBuilder() {
        LOG.debug("Create OIDC-OAuth client redirect action builder...");
        return new CesOidcClientRedirectActionBuilder();
    }

    @Bean
    @RefreshScope
    public SingleLogoutServiceMessageHandler oidcSingleLogoutServiceMessageHandler(
            ObjectProvider<HttpClient> httpClient,
            ObjectProvider<SingleLogoutMessageCreator> oidcSingleLogoutMessageCreator,
            ObjectProvider<ServicesManager> servicesManager,
            ObjectProvider<SingleLogoutServiceLogoutUrlBuilder> singleLogoutServiceLogoutUrlBuilder,
            ObjectProvider<AuthenticationServiceSelectionPlan> authenticationServiceSelectionPlan,
            ObjectProvider<CasConfigurationProperties> casProperties
    ) {
        return new CesOidcSingleLogoutServiceMessageHandler(httpClient.getObject(),
                oidcSingleLogoutMessageCreator.getObject(),
                servicesManager.getObject(),
                singleLogoutServiceLogoutUrlBuilder.getObject(),
                casProperties.getObject().getSlo().isAsynchronous(),
                authenticationServiceSelectionPlan.getObject(),
                casProperties.getObject().getAuthn().getOidc().getIssuer());
    }
}
