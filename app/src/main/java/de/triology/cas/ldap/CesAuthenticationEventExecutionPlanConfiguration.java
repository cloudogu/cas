package de.triology.cas.ldap;

import com.google.common.collect.Multimap;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.*;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.authentication.principal.PrincipalNameTransformerUtils;
import org.apereo.cas.authentication.support.DefaultLdapAccountStateHandler;
import org.apereo.cas.authentication.support.OptionalWarningLdapAccountStateHandler;
import org.apereo.cas.authentication.support.RejectResultCodeLdapPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.support.password.DefaultPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.support.password.GroovyPasswordPolicyHandlingStrategy;
import org.apereo.cas.authentication.support.password.PasswordEncoderUtils;
import org.apereo.cas.authentication.support.password.PasswordPolicyContext;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapPasswordPolicyProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.LoggingUtils;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResponseHandler;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.ext.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Class for creating and configuring the LDAP authentication handler for the CES.
 */
@Configuration("CesAuthenticationEventExecutionPlanConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ComponentScan("de.triology.cas.ldap")
public class CesAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(CesAuthenticationEventExecutionPlanConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Autowired
    CombinedGroupResolver groupResolver;

    @Override
    public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(cesGroupAwareLdapAuthenticationHandler());
        LOG.trace("Registered {}, registered authentication handlers: {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), plan.getAuthenticationHandlers());
    }

    /**
     * Creates, configures and initializes the LDAP authentication handler. The configuration is created using the
     * configuration from the cas.properties file.
     */
    @RefreshScope
    @Bean
    public AuthenticationHandler cesGroupAwareLdapAuthenticationHandler() {
        val ldapProperties = casProperties.getAuthn().getLdap().get(0);

        val multiMapAttributes = createPrincipalAttributes(ldapProperties);
        val authenticator = createAuthenticator(ldapProperties, multiMapAttributes);

        val handler = createCesLDAPAuthenticationHandler(ldapProperties, authenticator);
        configureLDAPAuthenticationHandler(handler, ldapProperties, multiMapAttributes, authenticator);

        handler.initialize();

        return handler;
    }

    private Multimap<String, Object> createPrincipalAttributes(LdapAuthenticationProperties ldapProperties) {
        val multiMapAttributes = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(ldapProperties.getPrincipalAttributeList());
        LOG.debug("Created and mapped principal attributes [{}] ...", multiMapAttributes);

        return multiMapAttributes;
    }

    private Authenticator createAuthenticator(LdapAuthenticationProperties ldapProperties, Multimap<String, Object> multiMapAttributes) {
        val authenticator = LdapUtils.newLdaptiveAuthenticator(ldapProperties);
        LOG.debug("Ldap authenticator configured with return attributes [{}] and baseDn [{}]",
                multiMapAttributes.keySet(), ldapProperties.getBaseDn());

        return authenticator;
    }

    private LdapAuthenticationHandler createCesLDAPAuthenticationHandler(LdapAuthenticationProperties ldapProperties, Authenticator authenticator) {
        val strategy = createLdapPasswordPolicyHandlingStrategy(ldapProperties);

        return new CesGroupAwareLdapAuthenticationHandler(ldapProperties.getName(), servicesManager.getObject(), PrincipalFactoryUtils.newPrincipalFactory(),
                getOrder(), authenticator, strategy, groupResolver);
    }

    private void configureLDAPAuthenticationHandler(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties, Multimap<String, Object> multiMapAttributes, Authenticator authenticator) {
        configureDNAttributes(handler, ldapProperties);

        appendAdditionalAttributes(ldapProperties, multiMapAttributes);

        handler.setAllowMultiplePrincipalAttributeValues(ldapProperties.isAllowMultiplePrincipalAttributeValues());
        handler.setAllowMissingPrincipalAttributeValue(ldapProperties.isAllowMissingPrincipalAttributeValue());

        handler.setPasswordEncoder(PasswordEncoderUtils.newPasswordEncoder(ldapProperties.getPasswordEncoder(), applicationContext));
        handler.setPrincipalNameTransformer(PrincipalNameTransformerUtils.newPrincipalNameTransformer(ldapProperties.getPrincipalTransformation()));

        configureCredentialCriteria(handler, ldapProperties);
        configurePrincipalAttributeId(handler, ldapProperties);
        configurePasswordPolicy(handler, ldapProperties, authenticator, multiMapAttributes);

        val attributes = CollectionUtils.wrap(multiMapAttributes);
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
            val additional = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(ldapProperties.getAdditionalAttributes());
            multiMapAttributes.putAll(additional);
        }
    }

    private void configureCredentialCriteria(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties) {
        if (StringUtils.isNotBlank(ldapProperties.getCredentialCriteria())) {
            LOG.debug("Ldap authentication is filtering credentials by [{}]", ldapProperties.getCredentialCriteria());
            handler.setCredentialSelectionPredicate(CoreAuthenticationUtils.newCredentialSelectionPredicate(ldapProperties.getCredentialCriteria()));
        }
    }

    private void configurePrincipalAttributeId(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties) {
        if (StringUtils.isBlank(ldapProperties.getPrincipalAttributeId())) {
            LOG.debug("No principal id attribute is found for LDAP authentication");
        } else {
            handler.setPrincipalIdAttribute(ldapProperties.getPrincipalAttributeId());
            LOG.debug("Using principal id attribute [{}] for LDAP authentication", ldapProperties.getPrincipalAttributeId());
        }
    }

    private void configurePasswordPolicy(LdapAuthenticationHandler handler, LdapAuthenticationProperties ldapProperties, Authenticator authenticator, Multimap<String, Object> multiMapAttributes) {
        val passwordPolicy = ldapProperties.getPasswordPolicy();
        if (passwordPolicy.isEnabled()) {
            LOG.debug("Password policy is enabled. Constructing password policy configuration");
            val cfg = createLdapPasswordPolicyConfiguration(passwordPolicy, authenticator, multiMapAttributes);
            handler.setPasswordPolicyConfiguration(cfg);
        }
    }

    /**
     * This code is an exact 1:1 copy from {@link org.apereo.cas.config.LdapAuthenticationConfiguration}
     *
     * @see org.apereo.cas.config.LdapAuthenticationConfiguration
     **/
    private static PasswordPolicyContext createLdapPasswordPolicyConfiguration(final LdapPasswordPolicyProperties passwordPolicy,
                                                                               final Authenticator authenticator,
                                                                               final Multimap<String, Object> attributes) {
        val cfg = new PasswordPolicyContext(passwordPolicy);
        val handlers = new HashSet<>();

        val customPolicyClass = passwordPolicy.getCustomPolicyClass();
        if (StringUtils.isNotBlank(customPolicyClass)) {
            try {
                LOG.debug("Configuration indicates use of a custom password policy handler [{}]", customPolicyClass);
                val clazz = (Class<AuthenticationResponseHandler>) Class.forName(customPolicyClass);
                handlers.add(clazz.getDeclaredConstructor().newInstance());
            } catch (final Exception e) {
                LoggingUtils.warn(LOG, "Unable to construct an instance of the password policy handler", e);
            }
        }
        LOG.debug("Password policy authentication response handler is set to accommodate directory type: [{}]", passwordPolicy.getType());
        switch (passwordPolicy.getType()) {
            case AD:
                handlers.add(new ActiveDirectoryAuthenticationResponseHandler(Period.ofDays(cfg.getPasswordWarningNumberOfDays())));
                Arrays.stream(ActiveDirectoryAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOG.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
                    attributes.put(a, a);
                });
                break;
            case FreeIPA:
                Arrays.stream(FreeIPAAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOG.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
                    attributes.put(a, a);
                });
                handlers.add(new FreeIPAAuthenticationResponseHandler(
                        Period.ofDays(cfg.getPasswordWarningNumberOfDays()), cfg.getLoginFailures()));
                break;
            case EDirectory:
                Arrays.stream(EDirectoryAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOG.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
                    attributes.put(a, a);
                });
                handlers.add(new EDirectoryAuthenticationResponseHandler(Period.ofDays(cfg.getPasswordWarningNumberOfDays())));
                break;
            default:
                handlers.add(new PasswordPolicyAuthenticationResponseHandler());
                handlers.add(new PasswordExpirationAuthenticationResponseHandler());
                break;
        }
        authenticator.setResponseHandlers(handlers.toArray(AuthenticationResponseHandler[]::new));

        LOG.debug("LDAP authentication response handlers configured are: [{}]", handlers);

        if (!passwordPolicy.isAccountStateHandlingEnabled()) {
            cfg.setAccountStateHandler((response, configuration) -> new ArrayList<>(0));
            LOG.debug("Handling LDAP account states is disabled via CAS configuration");
        } else if (StringUtils.isNotBlank(passwordPolicy.getWarningAttributeName()) && StringUtils.isNotBlank(passwordPolicy.getWarningAttributeValue())) {
            val accountHandler = new OptionalWarningLdapAccountStateHandler();
            accountHandler.setDisplayWarningOnMatch(passwordPolicy.isDisplayWarningOnMatch());
            accountHandler.setWarnAttributeName(passwordPolicy.getWarningAttributeName());
            accountHandler.setWarningAttributeValue(passwordPolicy.getWarningAttributeValue());
            accountHandler.setAttributesToErrorMap(passwordPolicy.getPolicyAttributes());
            cfg.setAccountStateHandler(accountHandler);
            LOG.debug("Configuring an warning account state handler for LDAP authentication for warning attribute [{}] and value [{}]",
                    passwordPolicy.getWarningAttributeName(), passwordPolicy.getWarningAttributeValue());
        } else {
            val accountHandler = new DefaultLdapAccountStateHandler();
            accountHandler.setAttributesToErrorMap(passwordPolicy.getPolicyAttributes());
            cfg.setAccountStateHandler(accountHandler);
            LOG.debug("Configuring the default account state handler for LDAP authentication");
        }
        return cfg;
    }

    /**
     * This code is an exact 1:1 copy from {@link org.apereo.cas.config.LdapAuthenticationConfiguration}
     *
     * @see org.apereo.cas.config.LdapAuthenticationConfiguration
     **/
    private AuthenticationPasswordPolicyHandlingStrategy<AuthenticationResponse, PasswordPolicyContext>
    createLdapPasswordPolicyHandlingStrategy(final LdapAuthenticationProperties l) {
        if (l.getPasswordPolicy().getStrategy() == LdapPasswordPolicyProperties.PasswordPolicyHandlingOptions.REJECT_RESULT_CODE) {
            LOG.debug("Created LDAP password policy handling strategy based on blocked authentication result codes");
            return new RejectResultCodeLdapPasswordPolicyHandlingStrategy();
        }

        val location = l.getPasswordPolicy().getGroovy().getLocation();
        if (l.getPasswordPolicy().getStrategy() == LdapPasswordPolicyProperties.PasswordPolicyHandlingOptions.GROOVY && location != null) {
            LOG.debug("Created LDAP password policy handling strategy based on Groovy script [{}]", location);
            return new GroovyPasswordPolicyHandlingStrategy(location, applicationContext);
        }

        LOG.debug("Created default LDAP password policy handling strategy");
        return new DefaultPasswordPolicyHandlingStrategy();
    }
}
