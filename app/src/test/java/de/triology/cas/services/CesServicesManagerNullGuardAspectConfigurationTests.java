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

    /**
     * Simulates a {@link ServicesManager#load()} invocation that throws an NPE. The safety contract
     * requires the advice to let the original failure escape so startup cannot continue with an
     * apparently valid but empty service collection.
     */
    @Test
    void servicesManagerLoadDoesNotSwallowNpe() throws Throwable {
        ProceedingJoinPoint joinPoint = failingJoinPoint(mock(ServicesManager.class));

        assertThrows(
                NullPointerException.class,
                () -> aspect.guardServicesManagerLoad(joinPoint)
        );
    }

    /**
     * Exercises the equivalent guard around a service-registry load. This verifies that registry
     * failures also remain visible instead of being converted into an empty collection.
     */
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
