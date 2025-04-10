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
public class CesLdapPasswordManagementServiceTest {

    /**
     * Extension of the class {@link CesLdapPasswordManagementService} to be tested.
     * <p>
     * Since one method to test calls a super method, which - if at all - is very difficult to mock, this is simply
     * overwritten, as this is simply easier
     */
    class CesLdapPasswordManagementServiceForUnitTest extends CesLdapPasswordManagementService {

        /**
         * The email address to be returned by the findAttribute method.
         */
        private String emailToReturn;

        /**
         * Constructor.
         */
        public CesLdapPasswordManagementServiceForUnitTest(
            CipherExecutor<Serializable, String> cipherExecutor,
            CasConfigurationProperties casProperties,
            PasswordManagementProperties passwordManagementProperties,
            PasswordHistoryService passwordHistoryService,
            Map<String, ConnectionFactory> connectionFactoryMap) {
            super(cipherExecutor, casProperties, passwordManagementProperties, passwordHistoryService, connectionFactoryMap);
        }        

        /**
         * Sets the email address to be returned by the findAttribute method.
         *
         * @param emailToReturn the email address to set
         */
        public void setEmailToReturn(String emailToReturn) {
            this.emailToReturn = emailToReturn;
        }

        /**
         * Overridden method that simply returns the set email address.
         */
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
        cesLdapPasswordManagementService = new CesLdapPasswordManagementServiceForUnitTest(cipherExecutor, casConfigurationProperties, passwordManagementProperties, passwordHistoryService, null);

        when(passwordManagementProperties.getReset()).thenReturn(resetPasswordManagementProperties);
        when(resetPasswordManagementProperties.getMail()).thenReturn(emailProperties);
        when(emailProperties.getAttributeName()).thenReturn(Collections.singletonList("mail"));
    }

    @Test
    public void findEmailReturnsEMailWithEndingDotCom() {
        String email = "dustin@cloudogu.com";
        cesLdapPasswordManagementService.setEmailToReturn(email);

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(foundEMail, email);
    }

    @Test
    public void findEmailReturnsEMailWithEndingCesDotLocal() {
        String email = "dustin@ces.local";
        cesLdapPasswordManagementService.setEmailToReturn(email);

        String foundEMail = cesLdapPasswordManagementService.findEmail(passwordManagementQuery);
        assertEquals(foundEMail, email);
    }
}
