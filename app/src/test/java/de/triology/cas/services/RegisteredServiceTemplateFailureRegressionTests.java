package de.triology.cas.services;

import org.apereo.cas.services.CasRegisteredService;
import org.apereo.cas.services.DefaultRegisteredServiceProperty;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.RegisteredServiceProperty;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.util.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Regression tests for the service-registry failure
 */
class RegisteredServiceTemplateFailureRegressionTests {

    @Test
    void unresolvedServicesReproduceCas727ComparatorFailure() {
        CasRegisteredService first = unresolvedCasService(1, "first");
        CasRegisteredService second = unresolvedCasService(2, "second");

        assertNull(first.getServiceId());
        assertNull(second.getServiceId());

        NullPointerException failure = assertThrows(
                NullPointerException.class,
                () -> List.of(first, second).stream().sorted().toList()
        );

        assertComparatorFailure(failure);
    }

    @Test
    void missingTemplatesLeaveOAuthServiceInComparatorFailureState() {
        @SuppressWarnings("unchecked")
        StringSerializer<RegisteredService> serializer = mock(StringSerializer.class);
        var templatesManager = new CesLegacyCompatibleTemplatesManager(Collections.emptyList(), serializer);

        OAuthRegisteredService unresolved = new OAuthRegisteredService();
        unresolved.setId(13);
        unresolved.setTemplateName("BaseService,DefaultAttributeReleasePolicy,DefaultOAuthService");
        unresolved.setProperties(templateProperties("whiteboard"));

        RegisteredService result = templatesManager.apply(unresolved);

        assertNull(result.getName(), "The CAS-only fallback does not repair OAuth services");
        assertNull(result.getServiceId(), "Without BaseService expansion, serviceId remains null");

        OAuthRegisteredService comparisonPeer = new OAuthRegisteredService();
        comparisonPeer.setId(14);
        comparisonPeer.setTemplateName("BaseService,DefaultAttributeReleasePolicy,DefaultOAuthService");
        comparisonPeer.setProperties(templateProperties("second-oauth-service"));

        RegisteredService secondResult = templatesManager.apply(comparisonPeer);
        assertNull(secondResult.getServiceId());

        NullPointerException failure = assertThrows(
                NullPointerException.class,
                () -> List.of(result, secondResult).stream().sorted().toList()
        );

        assertComparatorFailure(failure);
    }

    private static CasRegisteredService unresolvedCasService(long id, String serviceName) {
        CasRegisteredService service = new CasRegisteredService();
        service.setId(id);
        service.setTemplateName("BaseService,DefaultAttributeReleasePolicy,AllowProxyPolicy");
        service.setProperties(templateProperties(serviceName));
        return service;
    }

    private static Map<String, RegisteredServiceProperty> templateProperties(String serviceName) {
        return Map.of(
                "ServiceName", new DefaultRegisteredServiceProperty(serviceName),
                "Fqdn", new DefaultRegisteredServiceProperty("example.org")
        );
    }

    private static void assertComparatorFailure(NullPointerException failure) {
        assertTrue(
                Arrays.stream(failure.getStackTrace())
                        .anyMatch(frame -> frame.getClassName().equals("org.apereo.cas.services.BaseRegisteredService")
                                && frame.getMethodName().equals("compareTo")),
                "The failure should originate in BaseRegisteredService.compareTo"
        );
    }
}
