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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
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
public class CesAuthenticationEventExecutionPlanConfiguration implements AuthenticationEventExecutionPlanConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesAuthenticationEventExecutionPlanConfiguration.class);

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("servicesManager")
    private ObjectProvider<ServicesManager> servicesManager;

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.groups}")
    private String groupAttribute;

    @RefreshScope
    @Bean
    // Mostly copied from LdapAuthenticationConfiguration, but uses its own AuthenticationHandler
    public AuthenticationHandler cesGroupAwareLdapAuthenticationHandler() {
        val l = casProperties.getAuthn().getLdap().get(0);

        val multiMapAttributes = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(l.getPrincipalAttributeList());
        LOGGER.debug("Created and mapped principal attributes [{}] for [{}]...", multiMapAttributes, l.getLdapUrl());

        LOGGER.debug("Creating LDAP authenticator for [{}] and baseDn [{}]", l.getLdapUrl(), l.getBaseDn());
        val authenticator = LdapUtils.newLdaptiveAuthenticator(l);
        LOGGER.debug("Ldap authenticator configured with return attributes [{}] for [{}] and baseDn [{}]",
                multiMapAttributes.keySet(), l.getLdapUrl(), l.getBaseDn());

        LOGGER.debug("Creating LDAP password policy handling strategy for [{}]", l.getLdapUrl());
        val strategy = createLdapPasswordPolicyHandlingStrategy(l);

        LOGGER.debug("Creating LDAP authentication handler for [{}]", l.getLdapUrl());
        val handler = new CesGroupAwareLdapAuthenticationHandler(l.getName(),
                servicesManager.getObject(), PrincipalFactoryUtils.newPrincipalFactory(),
                getOrder(), authenticator, strategy, groupAttribute);
        handler.setCollectDnAttribute(l.isCollectDnAttribute());

        if (!l.getAdditionalAttributes().isEmpty()) {
            val additional = CoreAuthenticationUtils.transformPrincipalAttributesListIntoMultiMap(l.getAdditionalAttributes());
            multiMapAttributes.putAll(additional);
        }
        if (StringUtils.isNotBlank(l.getPrincipalDnAttributeName())) {
            handler.setPrincipalDnAttributeName(l.getPrincipalDnAttributeName());
        }
        handler.setAllowMultiplePrincipalAttributeValues(l.isAllowMultiplePrincipalAttributeValues());
        handler.setAllowMissingPrincipalAttributeValue(l.isAllowMissingPrincipalAttributeValue());
        handler.setPasswordEncoder(PasswordEncoderUtils.newPasswordEncoder(l.getPasswordEncoder(), applicationContext));
        handler.setPrincipalNameTransformer(PrincipalNameTransformerUtils.newPrincipalNameTransformer(l.getPrincipalTransformation()));

        if (StringUtils.isNotBlank(l.getCredentialCriteria())) {
            LOGGER.trace("Ldap authentication for [{}] is filtering credentials by [{}]",
                    l.getLdapUrl(), l.getCredentialCriteria());
            handler.setCredentialSelectionPredicate(CoreAuthenticationUtils.newCredentialSelectionPredicate(l.getCredentialCriteria()));
        }

        if (StringUtils.isBlank(l.getPrincipalAttributeId())) {
            LOGGER.trace("No principal id attribute is found for LDAP authentication via [{}]", l.getLdapUrl());
        } else {
            handler.setPrincipalIdAttribute(l.getPrincipalAttributeId());
            LOGGER.trace("Using principal id attribute [{}] for LDAP authentication via [{}]", l.getPrincipalAttributeId(), l.getLdapUrl());
        }

        val passwordPolicy = l.getPasswordPolicy();
        if (passwordPolicy.isEnabled()) {
            LOGGER.trace("Password policy is enabled for [{}]. Constructing password policy configuration", l.getLdapUrl());
            val cfg = createLdapPasswordPolicyConfiguration(passwordPolicy, authenticator, multiMapAttributes);
            handler.setPasswordPolicyConfiguration(cfg);
        }

        val attributes = CollectionUtils.wrap(multiMapAttributes);
        handler.setPrincipalAttributeMap(attributes);

        LOGGER.debug("Initializing LDAP authentication handler for [{}]", l.getLdapUrl());
        handler.initialize();

        return handler;
    }

    @Override
    public void configureAuthenticationExecutionPlan(AuthenticationEventExecutionPlan plan) {
        plan.registerAuthenticationHandler(cesGroupAwareLdapAuthenticationHandler());
        LOGGER.trace("Registered {}, registered authentication handlers: {}",
                CesGroupAwareLdapAuthenticationHandler.class.getSimpleName(), plan.getAuthenticationHandlers());
    }

    // 1:1 copy from LdapAuthenticationConfiguration
    private static PasswordPolicyContext createLdapPasswordPolicyConfiguration(final LdapPasswordPolicyProperties passwordPolicy,
                                                                               final Authenticator authenticator,
                                                                               final Multimap<String, Object> attributes) {
        val cfg = new PasswordPolicyContext(passwordPolicy);
        val handlers = new HashSet<>();

        val customPolicyClass = passwordPolicy.getCustomPolicyClass();
        if (StringUtils.isNotBlank(customPolicyClass)) {
            try {
                LOGGER.debug("Configuration indicates use of a custom password policy handler [{}]", customPolicyClass);
                val clazz = (Class<AuthenticationResponseHandler>) Class.forName(customPolicyClass);
                handlers.add(clazz.getDeclaredConstructor().newInstance());
            } catch (final Exception e) {
                LoggingUtils.warn(LOGGER, "Unable to construct an instance of the password policy handler", e);
            }
        }
        LOGGER.debug("Password policy authentication response handler is set to accommodate directory type: [{}]", passwordPolicy.getType());
        switch (passwordPolicy.getType()) {
            case AD:
                handlers.add(new ActiveDirectoryAuthenticationResponseHandler(Period.ofDays(cfg.getPasswordWarningNumberOfDays())));
                Arrays.stream(ActiveDirectoryAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOGGER.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
                    attributes.put(a, a);
                });
                break;
            case FreeIPA:
                Arrays.stream(FreeIPAAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOGGER.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
                    attributes.put(a, a);
                });
                handlers.add(new FreeIPAAuthenticationResponseHandler(
                        Period.ofDays(cfg.getPasswordWarningNumberOfDays()), cfg.getLoginFailures()));
                break;
            case EDirectory:
                Arrays.stream(EDirectoryAuthenticationResponseHandler.ATTRIBUTES).forEach(a -> {
                    LOGGER.debug("Configuring authentication to retrieve password policy attribute [{}]", a);
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

        LOGGER.debug("LDAP authentication response handlers configured are: [{}]", handlers);

        if (!passwordPolicy.isAccountStateHandlingEnabled()) {
            cfg.setAccountStateHandler((response, configuration) -> new ArrayList<>(0));
            LOGGER.trace("Handling LDAP account states is disabled via CAS configuration");
        } else if (StringUtils.isNotBlank(passwordPolicy.getWarningAttributeName()) && StringUtils.isNotBlank(passwordPolicy.getWarningAttributeValue())) {
            val accountHandler = new OptionalWarningLdapAccountStateHandler();
            accountHandler.setDisplayWarningOnMatch(passwordPolicy.isDisplayWarningOnMatch());
            accountHandler.setWarnAttributeName(passwordPolicy.getWarningAttributeName());
            accountHandler.setWarningAttributeValue(passwordPolicy.getWarningAttributeValue());
            accountHandler.setAttributesToErrorMap(passwordPolicy.getPolicyAttributes());
            cfg.setAccountStateHandler(accountHandler);
            LOGGER.debug("Configuring an warning account state handler for LDAP authentication for warning attribute [{}] and value [{}]",
                    passwordPolicy.getWarningAttributeName(), passwordPolicy.getWarningAttributeValue());
        } else {
            val accountHandler = new DefaultLdapAccountStateHandler();
            accountHandler.setAttributesToErrorMap(passwordPolicy.getPolicyAttributes());
            cfg.setAccountStateHandler(accountHandler);
            LOGGER.debug("Configuring the default account state handler for LDAP authentication");
        }
        return cfg;
    }

    // 1:1 copy from LdapAuthenticationConfiguration
    private AuthenticationPasswordPolicyHandlingStrategy<AuthenticationResponse, PasswordPolicyContext>
    createLdapPasswordPolicyHandlingStrategy(final LdapAuthenticationProperties l) {
        if (l.getPasswordPolicy().getStrategy() == LdapPasswordPolicyProperties.PasswordPolicyHandlingOptions.REJECT_RESULT_CODE) {
            LOGGER.debug("Created LDAP password policy handling strategy based on blocked authentication result codes");
            return new RejectResultCodeLdapPasswordPolicyHandlingStrategy();
        }

        val location = l.getPasswordPolicy().getGroovy().getLocation();
        if (l.getPasswordPolicy().getStrategy() == LdapPasswordPolicyProperties.PasswordPolicyHandlingOptions.GROOVY && location != null) {
            LOGGER.debug("Created LDAP password policy handling strategy based on Groovy script [{}]", location);
            return new GroovyPasswordPolicyHandlingStrategy(location, applicationContext);
        }

        LOGGER.debug("Created default LDAP password policy handling strategy");
        return new DefaultPasswordPolicyHandlingStrategy();
    }
}
