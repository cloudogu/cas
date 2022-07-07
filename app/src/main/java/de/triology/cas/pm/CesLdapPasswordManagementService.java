package de.triology.cas.pm;

import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.pm.LdapPasswordManagementService;
import org.apereo.cas.pm.PasswordHistoryService;
import org.apereo.cas.pm.PasswordManagementQuery;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.ldaptive.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Extends the class {@link LdapPasswordManagementService}.
 * <p>
 * In the original classes, email addresses are validated and even with supposedly valid email addresses (e.g.
 * admin@ces.local) the validation fails.
 * <p>
 * As CAS should not exclude any email addresses and the emails should be sent to all email addresses, validation is
 * deactivated. The method responsible for this has been adapted accordingly.
 */
public class CesLdapPasswordManagementService extends LdapPasswordManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesLdapPasswordManagementService.class.getName());

    public CesLdapPasswordManagementService(final CipherExecutor<Serializable, String> cipherExecutor,
                                            final String issuer,
                                            final PasswordManagementProperties passwordManagementProperties,
                                            final PasswordHistoryService passwordHistoryService,
                                            final Map<String, ConnectionFactory> connectionFactoryMap) {
        super(cipherExecutor, issuer, passwordManagementProperties, passwordHistoryService, connectionFactoryMap);
    }

    @Override
    public String findEmail(final PasswordManagementQuery query) {
        val email = findAttribute(query, List.of(properties.getReset().getMail().getAttributeName()),
                CollectionUtils.wrap(query.getUsername()));

        if (StringUtils.isEmpty(email)) {
            LOGGER.warn("Email address [{}] for [{}] is empty", email, query.getUsername());
            return null;
        }

        return email;
    }
}
