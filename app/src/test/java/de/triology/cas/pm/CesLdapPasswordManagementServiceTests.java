package de.triology.cas.pm;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.email.EmailProperties;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.configuration.model.support.pm.ResetPasswordManagementProperties;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.ConnectionFactory;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CesLdapPasswordManagementServiceTests {

    class CesLdapPasswordManagementServiceForUnitTest extends CesLdapPasswordManagementService {
        private String emailToReturn;

        public CesLdapPasswordManagementServiceForUnitTest(
            CipherExecutor<Serializable, String> cipherExecutor,
            CasConfigurationProperties casProperties,
            PasswordManagementProperties passwordManagementProperties,
            PasswordHistoryService passwordHistoryService,
            Map<String, ConnectionFactory> connectionFactoryMap) {
            super(cipherExecutor, casProperties, passwordManagementProperties, passwordHistoryService, connectionFactoryMap);
        }

        public void setEmailToReturn(String emailToReturn) {
            this.emailToReturn = emailToReturn;
        }

        @Override
        protected String findAttribute(final PasswordManagementQuery context, final List<String> attributeNames, final List<String> ldapFilterParam) {
            return emailToReturn;
        }
    }

    @Mock
    private CasConfigurationProperties casConfigurationProperties;

    @Mock
    private PasswordManagementQuery passwordManagementQuery;

    @Mock
    private CipherExecutor<Serializable, String> cipherExecutor;

    private PasswordManagementProperties passwordManagementProperties = new PasswordManagementProperties();

    @Mock
    private ResetPasswordManagementProperties resetPasswordManagementProperties;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private PasswordHistoryService passwordHistoryService;

    private CesLdapPasswordManagementServiceForUnitTest cesLdapPasswordManagementService;

    @Before
    public void setup() {
        cesLdapPasswordManagementService = new CesLdapPasswordManagementServiceForUnitTest(
            cipherExecutor,
            casConfigurationProperties,
            passwordManagementProperties,
            passwordHistoryService,
            null
        );

        when(passwordManagementProperties.getReset()).thenReturn(resetPasswordManagementProperties);
        when(resetPasswordManagementProperties.getMail()).thenReturn(emailProperties);
        when(emailProperties.getAttributeName()).thenReturn(Collections.singletonList("mail"));
    }

    @Test
    public void findEmailReturnsNormalEmail() {
        String email = "dustin@cloudogu.com";
        cesLdapPasswordManagementService.setEmailToReturn(email);

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(email, foundEMail);
    }

    @Test
    public void findEmailReturnsLocalEmail() {
        String email = "dustin@ces.local";
        cesLdapPasswordManagementService.setEmailToReturn(email);

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(email, foundEMail);
    }

    @Test
    public void findEmailReturnsNullWhenEmptyString() {
        cesLdapPasswordManagementService.setEmailToReturn(""); // empty string

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(null, foundEMail);
    }

    @Test
    public void findEmailReturnsNullWhenNull() {
        cesLdapPasswordManagementService.setEmailToReturn(null); // real null

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(null, foundEMail);
    }
}
