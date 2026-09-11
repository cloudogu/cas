package de.triology.cas.services;

import org.apereo.cas.services.ServiceRegistry;
import org.apereo.cas.services.ServicesManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CesServicesManagerNullGuardAspectConfigurationTests {

    private final CesServicesManagerNullGuardAspectConfiguration aspect =
            new CesServicesManagerNullGuardAspectConfiguration();

    @Test
    void servicesManagerLoadDoesNotSwallowNpe() throws Throwable {
        ProceedingJoinPoint joinPoint = failingJoinPoint(mock(ServicesManager.class));

        assertThrows(
                NullPointerException.class,
                () -> aspect.guardServicesManagerLoad(joinPoint)
        );
    }

    @Test
    void serviceRegistryLoadDoesNotSwallowNpe() throws Throwable {
        ProceedingJoinPoint joinPoint = failingJoinPoint(mock(ServiceRegistry.class));

        assertThrows(
                NullPointerException.class,
                () -> aspect.guardServiceRegistryLoad(joinPoint)
        );
    }

    private static ProceedingJoinPoint failingJoinPoint(Object target) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.proceed()).thenThrow(new NullPointerException("service loading failed"));
        return joinPoint;
    }
}
