package de.triology.cas.pm;

import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apereo.cas.configuration.model.support.pm.PasswordManagementProperties;
import org.apereo.cas.pm.BasePasswordManagementService;
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

public class CesLdapPasswordManagementService extends LdapPasswordManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CesSendPasswordResetInstructionsAction.class.getName());

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
