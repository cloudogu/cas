package de.triology.cas.services;

import org.apereo.cas.services.ServicesManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CesServicesManagerNullGuardAspectConfiguration}.
 * <p>
 * The two advice methods have identical bodies, so each case is parameterized over both.
 */
class CesServicesManagerNullGuardAspectConfigurationTest {

    private CesServicesManagerNullGuardAspectConfiguration aspect;
    private ProceedingJoinPoint pjp;

    private interface GuardMethod {
        Object invoke(CesServicesManagerNullGuardAspectConfiguration aspect, ProceedingJoinPoint pjp) throws Throwable;
    }

    private static Stream<Arguments> guardMethods() {
        return Stream.of(
                Arguments.of("guardServicesManagerLoad", (GuardMethod) CesServicesManagerNullGuardAspectConfiguration::guardServicesManagerLoad),
                Arguments.of("guardServiceRegistryLoad", (GuardMethod) CesServicesManagerNullGuardAspectConfiguration::guardServiceRegistryLoad)
        );
    }

    @BeforeEach
    void setUp() {
        aspect = new CesServicesManagerNullGuardAspectConfiguration();
        pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getTarget()).thenReturn(mock(ServicesManager.class));
    }

    @ParameterizedTest(name = "{0}: returns original result when non-null")
    @MethodSource("guardMethods")
    void returnsOriginalResult_WhenNonNull(String name, GuardMethod guardMethod) throws Throwable {
        List<String> loaded = List.of("service1");
        when(pjp.proceed()).thenReturn(loaded);

        Object result = guardMethod.invoke(aspect, pjp);

        assertEquals(loaded, result);
    }

    @ParameterizedTest(name = "{0}: returns empty list when result is null")
    @MethodSource("guardMethods")
    void returnsEmptyList_WhenResultIsNull(String name, GuardMethod guardMethod) throws Throwable {
        when(pjp.proceed()).thenReturn(null);

        Object result = guardMethod.invoke(aspect, pjp);

        assertEquals(List.of(), result);
    }

    @ParameterizedTest(name = "{0}: returns empty list when NullPointerException thrown")
    @MethodSource("guardMethods")
    void returnsEmptyList_WhenNullPointerExceptionThrown(String name, GuardMethod guardMethod) throws Throwable {
        when(pjp.proceed()).thenThrow(new NullPointerException("boom"));

        Object result = guardMethod.invoke(aspect, pjp);

        assertEquals(List.of(), result);
    }

    @ParameterizedTest(name = "{0}: propagates other Throwables")
    @MethodSource("guardMethods")
    void propagatesOtherThrowables(String name, GuardMethod guardMethod) throws Throwable {
        when(pjp.proceed()).thenThrow(new IllegalStateException("not an NPE"));

        assertThrows(IllegalStateException.class, () -> guardMethod.invoke(aspect, pjp));
    }
}
