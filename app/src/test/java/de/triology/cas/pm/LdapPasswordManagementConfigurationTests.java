package de.triology.cas.pm;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.config.CasLdapPasswordManagementAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LdapPasswordManagementConfigurationTests {

    private CasConfigurationProperties casProperties;
    private CipherExecutor<Serializable, String> cipherExecutor;
    private PasswordHistoryService passwordHistoryService;

    @BeforeEach
    void setUp() {
        casProperties = mock(CasConfigurationProperties.class);
        cipherExecutor = mock(CipherExecutor.class);
        passwordHistoryService = mock(PasswordHistoryService.class);
    
        var authnProps = mock(AuthenticationProperties.class);
        var pmProps = mock(PasswordManagementProperties.class);
        var serverProps = mock(org.apereo.cas.configuration.model.core.CasServerProperties.class);
    
        when(casProperties.getAuthn()).thenReturn(authnProps);
        when(authnProps.getPm()).thenReturn(pmProps);
    
        when(pmProps.getLdap()).thenReturn(List.of());
    
        when(casProperties.getServer()).thenReturn(serverProps);
        when(serverProps.getPrefix()).thenReturn("https://cas.example.org");
    }
    

    @Test
    void shouldCreateCesLdapPasswordManagementService() {
        var config = new LdapPasswordManagementConfiguration();
        var service = config.passwordChangeService(casProperties, cipherExecutor, passwordHistoryService);

        assertNotNull(service);
        assertInstanceOf(CesLdapPasswordManagementService.class, service);
    }

    /**
     * Regression guard for #345 / #163: the config must not be gated by the indexed
     * {@code @ConditionalOnProperty("cas.authn.pm.ldap[0].ldap-url")}, which stopped resolving
     * after the CAS 6->7 / Spring Boot 2->3 upgrade and silently deactivated the CES override.
     */
    @Test
    void shouldNotBeGatedByConditionalOnProperty() {
        assertNull(LdapPasswordManagementConfiguration.class.getAnnotation(ConditionalOnProperty.class),
                "@ConditionalOnProperty must not be present; the indexed [0] property broke registration of the CES override");
    }

    /**
     * The CES config must be applied before Apereo's auto-configuration so the CES bean wins
     * deterministically regardless of auto-configuration ordering.
     */
    @Test
    void shouldBeAppliedBeforeApereoAutoConfiguration() {
        var autoConfigureBefore = LdapPasswordManagementConfiguration.class.getAnnotation(AutoConfigureBefore.class);
        assertNotNull(autoConfigureBefore, "@AutoConfigureBefore must be present");
        assertTrue(List.of(autoConfigureBefore.value()).contains(CasLdapPasswordManagementAutoConfiguration.class),
                "@AutoConfigureBefore must target CasLdapPasswordManagementAutoConfiguration");
    }

    /**
     * Regression guard: the bean must stay registered under both names (see the wiring rationale on
     * the {@code @Bean} declaration in {@link LdapPasswordManagementConfiguration}).
     */
    @Test
    void shouldRegisterBeanUnderBothNames() throws NoSuchMethodException {
        Method method = LdapPasswordManagementConfiguration.class.getDeclaredMethod(
                "passwordChangeService", CasConfigurationProperties.class, CipherExecutor.class, PasswordHistoryService.class);
        var bean = method.getAnnotation(Bean.class);
        assertNotNull(bean, "@Bean must be present on passwordChangeService");
        var names = List.of(bean.name());
        assertTrue(names.contains("passwordChangeService"), "bean must be named passwordChangeService");
        assertTrue(names.contains("ldapPasswordChangeService"), "bean must also be named ldapPasswordChangeService");
    }
}
