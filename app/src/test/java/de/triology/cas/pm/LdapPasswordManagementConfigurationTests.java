package de.triology.cas.pm;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.core.authentication.AuthenticationProperties;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
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
        assertTrue(service instanceof CesLdapPasswordManagementService);
    }
}
