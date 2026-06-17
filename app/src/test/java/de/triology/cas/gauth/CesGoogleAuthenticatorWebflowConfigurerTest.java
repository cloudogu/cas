package de.triology.cas.gauth;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.junit.jupiter.api.Test;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CesGoogleAuthenticatorWebflowConfigurerTest {
    @Test
    void deleteDeviceStateReturnsErrorsToConfirmRegistrationViewAndKeepsSuccessTransition() {
        var casProperties = new CasConfigurationProperties();
        var flow = new Flow(casProperties.getAuthn().getMfa().getGauth().getId());
        var checkRegistration = new ActionState(flow, CasWebflowConstants.STATE_ID_CHECK_ACCOUNT_REGISTRATION);
        var confirmRegistration = new ActionState(flow, "viewConfirmRegistration");
        var deleteDevice = new ActionState(flow, "googleAccountDeleteDevice");
        deleteDevice.getTransitionSet().add(new org.springframework.webflow.engine.Transition(
            new org.springframework.webflow.engine.support.DefaultTransitionCriteria(
                new org.springframework.binding.expression.support.LiteralExpression(
                    CasWebflowConstants.TRANSITION_ID_SUCCESS)),
            new org.springframework.webflow.engine.support.DefaultTargetStateResolver(checkRegistration.getId())));

        var registry = mock(FlowDefinitionRegistry.class);
        when(registry.getFlowDefinitionIds()).thenReturn(new String[]{flow.getId()});
        when(registry.getFlowDefinition(flow.getId())).thenReturn(flow);

        var configurer = new CesGoogleAuthenticatorWebflowConfigurer(null, null, registry, null, casProperties);
        configurer.initialize();

        var errorTransition = deleteDevice.getTransition(CasWebflowConstants.TRANSITION_ID_ERROR);
        assertNotNull(errorTransition);
        assertEquals(confirmRegistration.getId(), errorTransition.getTargetStateId());

        var successTransition = deleteDevice.getTransition(CasWebflowConstants.TRANSITION_ID_SUCCESS);
        assertNotNull(successTransition);
        assertEquals(checkRegistration.getId(), successTransition.getTargetStateId());
    }
}
