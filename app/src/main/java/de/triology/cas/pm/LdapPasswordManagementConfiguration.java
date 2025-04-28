package de.triology.cas.pm;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordManagementService;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.crypto.CipherExecutor;

import lombok.val;
import org.ldaptive.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This is {@link LdapPasswordManagementConfiguration}.
 */
@Configuration(value = "LdapPasswordManagementConfiguration", proxyBeanMethods = false)
@ConditionalOnProperty(name = "cas.authn.pm.ldap[0].ldap-url")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class LdapPasswordManagementConfiguration {

    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @Bean
    public PasswordManagementService passwordChangeService(
            final CasConfigurationProperties casProperties,
            @Qualifier("passwordManagementCipherExecutor")
            final CipherExecutor passwordManagementCipherExecutor,
            @Qualifier("passwordHistoryService")
            final PasswordHistoryService passwordHistoryService) {
        val connectionFactoryMap = new ConcurrentHashMap<String, ConnectionFactory>();
        val passwordManagerProperties = casProperties.getAuthn().getPm();
        passwordManagerProperties.getLdap().forEach(ldap -> connectionFactoryMap.put(ldap.getLdapUrl(), LdapUtils.newLdaptiveConnectionFactory(ldap)));
        return new CesLdapPasswordManagementService(passwordManagementCipherExecutor, casProperties, passwordManagerProperties, passwordHistoryService,
                connectionFactoryMap);
    }
}