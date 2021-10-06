package de.triology.cas.oidc.config;

import de.triology.cas.oidc.beans.CesOidcClientRedirectActionBuilder;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.oidc.config.OidcConfiguration;
import org.apereo.cas.support.oauth.web.response.OAuth20CasClientRedirectActionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

}
