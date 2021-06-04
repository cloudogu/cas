package de.triology.cas.oauth;

import de.triology.cas.oauth.web.CesOAuth20CallbackAuthorizeController;
import de.triology.cas.oauth.web.CesOAuth20WrapperController;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration("OAuthConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan({"de.triology.cas.oauth", "de.triology.cas.services"})
@DependsOn({"servicesManager", "ticketRegistry"})
public class CesOAuthConfiguration {
    private final Logger logger = LoggerFactory.getLogger(CesOAuth20CallbackAuthorizeController.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    // IntelliJ does not detect that servicesManager, however it does exist
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("servicesManager")
    private ServicesManager servicesManager;

    // IntelliJ does not detect that ticketRegistry, however it does exist
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    @Qualifier("ticketRegistry")
    private TicketRegistry ticketRegistry;

    @Value("${cas.server.prefix}")
    private String loginUrl;

    @Value("${ces.services.oauth.sessionTimeout}")
    private long timeout;

    @RefreshScope
    @Bean("/oauth2.0/*")
    public CesOAuth20WrapperController myOAuthController() {
        logger.warn("OAUTH: create o auth controller : {}, {}}", servicesManager.toString(), ticketRegistry.toString());
        return new CesOAuth20WrapperController(servicesManager, ticketRegistry, loginUrl + "/login", timeout);
    }
}