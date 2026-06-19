package de.triology.cas.gauth;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.features.CasFeatureModule;
import org.apereo.cas.gauth.credential.BaseGoogleAuthenticatorTokenCredentialRepository;
import org.apereo.cas.gauth.credential.GoogleAuthenticatorTokenCredential;
import org.apereo.cas.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialValidator;
import org.apereo.cas.util.spring.boot.ConditionalOnFeatureEnabled;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * Registers the CES Google Authenticator device-removal customization.
 *
 * <p>The upstream CAS gauth auto-configuration creates {@code googleAccountDeleteDeviceAction} only when
 * that bean name is missing. Loading this auto-configuration before CAS's gauth auto-configuration lets
 * us replace just that action while keeping the rest of CAS's gauth setup intact.</p>
 */
@AutoConfiguration(beforeName = "org.apereo.cas.config.CasGoogleAuthenticatorAutoConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
@ConditionalOnFeatureEnabled(feature = CasFeatureModule.FeatureCatalog.GoogleAuthenticator)
public class CesGoogleAuthenticatorConfiguration {
    /*
     * CAS registers its gauth webflow configurer at order 100. Running this one immediately afterwards lets
     * us add the missing error transition to the already-created googleAccountDeleteDevice state.
     */
    private static final int WEBFLOW_CONFIGURER_ORDER = 101;

    /*
     * Use CAS's exact action id as the bean name. This is what makes the upstream
     * @ConditionalOnMissingBean(name = "googleAccountDeleteDeviceAction") back off.
     */
    @Bean(name = CasWebflowConstants.ACTION_ID_GOOGLE_ACCOUNT_DELETE_DEVICE)
    @ConditionalOnMissingBean(name = CasWebflowConstants.ACTION_ID_GOOGLE_ACCOUNT_DELETE_DEVICE)
    public Action googleAccountDeleteDeviceAction(
        @Qualifier("googleAuthenticatorOneTimeTokenCredentialValidator")
        final OneTimeTokenCredentialValidator<GoogleAuthenticatorTokenCredential, GoogleAuthenticatorToken> validator,
        @Qualifier(BaseGoogleAuthenticatorTokenCredentialRepository.BEAN_NAME)
        final OneTimeTokenCredentialRepository repository) {
        return new CesGoogleAuthenticatorDeleteAccountAction(repository, validator);
    }

    /*
     * The action returns "error" for invalid codes. CAS's stock gauth flow only wires success for this
     * state, so this configurer adds the missing route back to the confirm-registration page.
     */
    @Bean
    @ConditionalOnMissingBean(name = "cesGoogleAuthenticatorWebflowConfigurer")
    public CasWebflowConfigurer cesGoogleAuthenticatorWebflowConfigurer(
        final CasConfigurationProperties casProperties,
        final ConfigurableApplicationContext applicationContext,
        @Qualifier("googleAuthenticatorFlowRegistry")
        final FlowDefinitionRegistry googleAuthenticatorFlowRegistry,
        @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_DEFINITION_REGISTRY)
        final FlowDefinitionRegistry flowDefinitionRegistry,
        @Qualifier(CasWebflowConstants.BEAN_NAME_FLOW_BUILDER_SERVICES)
        final FlowBuilderServices flowBuilderServices) {
        var configurer = new CesGoogleAuthenticatorWebflowConfigurer(
            flowBuilderServices,
            flowDefinitionRegistry,
            googleAuthenticatorFlowRegistry,
            applicationContext,
            casProperties);
        configurer.setOrder(WEBFLOW_CONFIGURER_ORDER);
        return configurer;
    }

    @Bean
    @ConditionalOnMissingBean(name = "cesGoogleAuthenticatorWebflowExecutionPlanConfigurer")
    public CasWebflowExecutionPlanConfigurer cesGoogleAuthenticatorWebflowExecutionPlanConfigurer(
        @Qualifier("cesGoogleAuthenticatorWebflowConfigurer")
        final CasWebflowConfigurer configurer) {
        return plan -> plan.registerWebflowConfigurer(configurer);
    }
}

