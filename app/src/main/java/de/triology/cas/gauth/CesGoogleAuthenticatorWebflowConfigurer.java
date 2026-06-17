package de.triology.cas.gauth;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.AbstractCasWebflowConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

/**
 * Adds a validation-error path for Google Authenticator device deletion.
 *
 * <p>CAS 7.3's stock {@code googleAccountDeleteDevice} state only transitions on {@code success}. Our
 * replacement delete action deliberately returns {@code error} for wrong codes and unverified delete
 * attempts, so the gauth subflow needs an explicit error transition back to the confirmation view.</p>
 */
public class CesGoogleAuthenticatorWebflowConfigurer extends AbstractCasWebflowConfigurer {
    private static final String STATE_ID_VIEW_CONFIRM_REGISTRATION = "viewConfirmRegistration";

    private static final String STATE_ID_GOOGLE_ACCOUNT_DELETE_DEVICE = "googleAccountDeleteDevice";

    private final FlowDefinitionRegistry googleAuthenticatorFlowRegistry;

    public CesGoogleAuthenticatorWebflowConfigurer(
        final FlowBuilderServices flowBuilderServices,
        final FlowDefinitionRegistry flowDefinitionRegistry,
        final FlowDefinitionRegistry googleAuthenticatorFlowRegistry,
        final ConfigurableApplicationContext applicationContext,
        final CasConfigurationProperties casProperties) {
        super(flowBuilderServices, flowDefinitionRegistry, applicationContext, casProperties);
        this.googleAuthenticatorFlowRegistry = googleAuthenticatorFlowRegistry;
    }

    @Override
    protected void doInitialize() {
        /*
         * Google Authenticator is a separate MFA subflow, not the main login flow. Read it from the
         * gauth-specific registry that CAS exposes for this provider.
         */
        var providerId = casProperties.getAuthn().getMfa().getGauth().getId();
        var flow = getFlow(googleAuthenticatorFlowRegistry, providerId);
        if (flow == null || !containsFlowState(flow, STATE_ID_GOOGLE_ACCOUNT_DELETE_DEVICE)) {
            return;
        }

        /*
         * Leave CAS's success transition untouched. We only add the missing error route so invalid delete
         * attempts render the same confirmation page with our localized message.
         */
        var deleteDevice = getTransitionableState(flow, STATE_ID_GOOGLE_ACCOUNT_DELETE_DEVICE, ActionState.class);
        createTransitionForState(
            deleteDevice,
            CasWebflowConstants.TRANSITION_ID_ERROR,
            STATE_ID_VIEW_CONFIRM_REGISTRATION);
    }
}
