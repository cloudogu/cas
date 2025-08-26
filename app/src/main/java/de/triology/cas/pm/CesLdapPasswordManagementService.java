package de.triology.cas.pm;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.pm.LdapPasswordManagementService;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.ldaptive.ConnectionFactory;

import java.io.Serializable;
import java.util.Map;

import org.apereo.cas.configuration.CasConfigurationProperties;


/**
 * Extends the class {@link LdapPasswordManagementService}.
 * <p>
 * In the original classes, email addresses are validated and even with supposedly valid email addresses (e.g.
 * admin@ces.local) the validation fails.
 * <p>
 * As CAS should not exclude any email addresses and the emails should be sent to all email addresses, validation is
 * deactivated. The method responsible for this has been adapted accordingly.
 */
@Slf4j
public class CesLdapPasswordManagementService extends LdapPasswordManagementService {

    private final CasConfigurationProperties casProperties;
    private final PasswordManagementProperties passwordManagementProperties;
    
    public CesLdapPasswordManagementService(
            final CipherExecutor<Serializable, String> cipherExecutor,
            final CasConfigurationProperties casProperties,
            final PasswordManagementProperties passwordManagementProperties,
            final PasswordHistoryService passwordHistoryService,
            final Map<String, ConnectionFactory> connectionFactoryMap) {
        super(cipherExecutor,
        casProperties,
        passwordHistoryService,
        connectionFactoryMap);
          
        this.casProperties = casProperties;
        this.passwordManagementProperties = passwordManagementProperties;
    }

    @Override
    public String findEmail(final PasswordManagementQuery query) {
        val email = findAttribute(query,
                passwordManagementProperties.getReset().getMail().getAttributeName(),
                CollectionUtils.wrap(query.getUsername()));

        if (StringUtils.isEmpty(email)) {
            LOGGER.warn("Email address [{}] for [{}] is empty", email, query.getUsername());
            return null;
        }
        return email;
    }
}

