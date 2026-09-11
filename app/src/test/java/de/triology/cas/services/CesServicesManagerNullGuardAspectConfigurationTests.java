package de.triology.cas.services;

import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.ServicesManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CesServicesManagerNullGuardAspectConfigurationTests {

    private final CesServicesManagerNullGuardAspectConfiguration aspect =
            new CesServicesManagerNullGuardAspectConfiguration();

    @Test
    void servicesManagerLoadNpeIsConvertedToEmptyCollection() throws Throwable {
        ProceedingJoinPoint joinPoint = failingJoinPoint(mock(ServicesManager.class));

        Object result = aspect.guardServicesManagerLoad(joinPoint);

        assertTrue(((Collection<?>) result).isEmpty());
    }

    @Test
    void serviceRegistryLoadNpeIsConvertedToEmptyCollection() throws Throwable {
        ProceedingJoinPoint joinPoint = failingJoinPoint(mock(ServiceRegistry.class));

        Object result = aspect.guardServiceRegistryLoad(joinPoint);

        assertTrue(((Collection<?>) result).isEmpty());
    }

    private static ProceedingJoinPoint failingJoinPoint(Object target) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.proceed()).thenThrow(new NullPointerException("service loading failed"));
        return joinPoint;
    }
}
