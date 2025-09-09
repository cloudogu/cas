package de.triology.cas.ldap;

import com.google.common.collect.Multimap;
import de.triology.cas.ldap.resolvers.CombinedGroupResolver;
import de.triology.cas.ldap.resolvers.GroupResolver;
import de.triology.cas.ldap.resolvers.MemberGroupResolver;
import de.triology.cas.ldap.resolvers.MemberOfGroupResolver;
import de.triology.cas.principal.AttributeSelectingPrincipalFactory;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.AuthenticationHandler;
import org.apereo.cas.authentication.AuthenticationPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.CoreAuthenticationUtils;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalNameTransformerUtils;
import org.apereo.cas.authentication.support.password.PasswordEncoderUtils;
import org.apereo.cas.authentication.support.password.PasswordPolicyContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapPasswordPolicyProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration("GroupResolverConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@Slf4j
public class LdapConfiguration {

    @NotNull
    @Value("${cas.authn.ldap[0].search-filter}")
    private String searchFilter;

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.groups}")
    private String groupAttribute;

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.baseDn}")
    private String baseDN;

    @Bean(name = PrincipalFactory.BEAN_NAME)
    public PrincipalFactory principalFactory() {
        // Order matters: first match wins.
        List<String> candidates = List.of(
            "cn",                  // LDAP common name
            "preferred_username",  // Keycloak / OIDC
            "displayName",         // common LDAP/profile mapping
            "mail"                 // last resort (email)
        );

        return new AttributeSelectingPrincipalFactory(candidates.toArray(String[]::new));
    }

    @Bean
    @RefreshScope
    CombinedGroupResolver combinedGroupResolver(CasConfigurationProperties properties) {
        List<GroupResolver> groupResolvers = new ArrayList<>(2);
        groupResolvers.add(new MemberGroupResolver(baseDN, searchPooledLdapConnectionFactory(properties), searchFilter));
        groupResolvers.add(new MemberOfGroupResolver(groupAttribute));

        return new CombinedGroupResolver(groupResolvers);
    }

    ConnectionFactory searchPooledLdapConnectionFactory(CasConfigurationProperties properties) {
        var ldapProperties = properties.getAuthn().getLdap().getFirst();
        return LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);
    }

    /**
     * Creates, configures and initializes the LDAP authentication handler. The configuration is created using the
     * configuration from the cas.properties file.
     */
    @RefreshScope
    @Bean
    public AuthenticationHandler cesGroupAwareLdapAuthenticationHandler(CasConfigurationProperties casProperties,
                                                                        ConfigurableApplicationContext applicationContext,
                                                                        @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
                                                                                ServicesManager servicesManager,
                                                                        CombinedGroupResolver combinedGroupResolver) {
        LdapAuthenticationProperties ldapProperties = casProperties.getAuthn().getLdap().getFirst();

        Multimap<String, Object> multiMapAttributes = createPrincipalAttributes(ldapProperties);
        Authenticator authenticator = createAuthenticator(ldapProperties, multiMapAttributes);

        LdapAuthenticationHandler handler = createCesLDAPAuthenticationHandler(ldapProperties, authenticator, applicationContext, servicesManager, combinedGroupResolver);
        configureLDAPAuthenticationHandler(handler, ldapProperties, multiMapAttributes, authenticator, applicationContext);

        handler.initialize();

        return handler;
    }

    private Multimap<String, Object> createPrincipalAttributes(LdapAuthenticationProperties ldapProperties) {
        Multimap<String, Object> multiMapAttributes = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(ldapProperties.getPrincipalAttributeList());
        LOGGER.debug("Created and mapped principal attributes [{}] ...", multiMapAttributes);

        return multiMapAttributes;
    }

    private Authenticator createAuthenticator(LdapAuthenticationProperties ldapProperties,
                                              Multimap<String, Object> multiMapAttributes) {
        Authenticator authenticator = LdapUtils.newLdaptiveAuthenticator(ldapProperties);
        LOGGER.debug("Ldap authenticator configured with return attributes [{}] and baseDn [{}]",
                multiMapAttributes.keySet(), ldapProperties.getBaseDn());

        return authenticator;
    }

    private LdapAuthenticationHandler createCesLDAPAuthenticationHandler(LdapAuthenticationProperties ldapProperties,
                                                                         Authenticator authenticator,
                                                                         ConfigurableApplicationContext applicationContext,
                                                                         ServicesManager servicesManager,
                                                                         CombinedGroupResolver combinedGroupResolver) {
        AuthenticationPasswordPolicyHandlingStrategy<AuthenticationResponse, PasswordPolicyContext> strategy = LdapUtils.createLdapPasswordPolicyHandlingStrategy(ldapProperties, applicationContext);

        return new CesGroupAwareLdapAuthenticationHandler(ldapProperties.getName(), servicesManager, PrincipalFactoryUtils.newPrincipalFactory(),
                authenticator, strategy, combinedGroupResolver);
    }

    private void configureLDAPAuthenticationHandler(LdapAuthenticationHandler handler,
                                                    LdapAuthenticationProperties ldapProperties,
                                                    Multimap<String, Object> multiMapAttributes,
                                                    Authenticator authenticator,
                                                    ConfigurableApplicationContext applicationContext) {
        configureDNAttributes(handler, ldapProperties);

        appendAdditionalAttributes(ldapProperties, multiMapAttributes);

        handler.setAllowMultiplePrincipalAttributeValues(ldapProperties.isAllowMultiplePrincipalAttributeValues());
        handler.setAllowMissingPrincipalAttributeValue(ldapProperties.isAllowMissingPrincipalAttributeValue());

        handler.setPasswordEncoder(PasswordEncoderUtils.newPasswordEncoder(ldapProperties.getPasswordEncoder(), applicationContext));
        handler.setPrincipalNameTransformer(PrincipalNameTransformerUtils.newPrincipalNameTransformer(ldapProperties.getPrincipalTransformation()));

        configureCredentialCriteria(handler, ldapProperties);
        configurePrincipalAttributeId(handler, ldapProperties);
        configurePasswordPolicy(handler, ldapProperties, authenticator, multiMapAttributes);

        Map<String, Object> attributes = CollectionUtils.wrap(multiMapAttributes);
        handler.setPrincipalAttributeMap(attributes);
    }

    private void configureDNAttributes(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties) {
        handler.setCollectDnAttribute(ldapProperties.isCollectDnAttribute());
        if (StringUtils.isNotBlank(ldapProperties.getPrincipalDnAttributeName())) {
            handler.setPrincipalDnAttributeName(ldapProperties.getPrincipalDnAttributeName());
        }
    }

    private void appendAdditionalAttributes(LdapAuthenticationProperties ldapProperties, Multimap<String, Object> multiMapAttributes) {
        if (!ldapProperties.getAdditionalAttributes().isEmpty()) {
            Multimap<String, Object> additional = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(ldapProperties.getAdditionalAttributes());
            multiMapAttributes.putAll(additional);
        }
    }

    private void configureCredentialCriteria(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties) {
        if (StringUtils.isNotBlank(ldapProperties.getCredentialCriteria())) {
            LOGGER.debug("Ldap authentication is filtering credentials by [{}]", ldapProperties.getCredentialCriteria());
            handler.setCredentialSelectionPredicate(CoreAuthenticationUtils.newCredentialSelectionPredicate(ldapProperties.getCredentialCriteria()));
        }
    }

    private void configurePrincipalAttributeId(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties) {
        if (StringUtils.isBlank(ldapProperties.getPrincipalAttributeId())) {
            LOGGER.debug("No principal id attribute is found for LDAP authentication");
        } else {
            handler.setPrincipalIdAttribute(ldapProperties.getPrincipalAttributeId());
            LOGGER.debug("Using principal id attribute [{}] for LDAP authentication", ldapProperties.getPrincipalAttributeId());
        }
    }

    private void configurePasswordPolicy(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties, Authenticator authenticator, Multimap<String, Object> multiMapAttributes) {
        LdapPasswordPolicyProperties passwordPolicy = ldapProperties.getPasswordPolicy();
        if (passwordPolicy.isEnabled()) {
            LOGGER.debug("Password policy is enabled. Constructing password policy configuration");
            PasswordPolicyContext cfg = LdapUtils.createLdapPasswordPolicyConfiguration(passwordPolicy, authenticator, multiMapAttributes);
            handler.setPasswordPolicyConfiguration(cfg);
        }
    }
}
