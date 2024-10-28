package de.triology.cas.ldap;

import lombok.extern.slf4j.Slf4j;
import org.ldaptive.*;

@Slf4j
public class UserManager {
    public static final String LDAP_TRUE = "TRUE";
    public static final String LDAP_FALSE = "FALSE";

    public static final String ObjectClassAttributeName = "objectClass";

    private final String baseDN;
    private final ConnectionFactory connectionFactory;

    public UserManager(String baseDN, ConnectionFactory connectionFactory) {
        this.baseDN = baseDN;
        this.connectionFactory = connectionFactory;
    }

    public CesInternalLdapUser getUserByUid(String uid) throws CesLdapException {
        final SearchResponse response;
        try {
            final SearchRequest request = createGetUserRequest(uid);
            response = new SearchOperation(connectionFactory).execute(request);
        } catch (final LdapException e) {
            throw new CesLdapException("Failed executing LDAP query ", e);
        }

        if (!response.isSuccess()) {
            throw new CesLdapException(response.getDiagnosticMessage());
        }

        if (response.getEntries().size() > 1) {
            throw new CesLdapException("did not expect more then one result");
        }

        LdapEntry entry = response.getEntry();
        if (entry == null) {
            return null;
        }

        return CesInternalLdapUser.UserFromEntry(entry);
    }


    public void createUser(CesInternalLdapUser user) throws CesLdapException {
        try {
            final AddOperation modify = new AddOperation(this.connectionFactory);
            final AddRequest request = AddRequest.builder()
                    .dn(createDnForUser(user))
                    .attributes(
                            new LdapAttribute(ObjectClassAttributeName, CesInternalLdapUser.ObjectClasses),
                            new LdapAttribute(CesInternalLdapUser.CnAttribute, user.getUid()),
                            new LdapAttribute(CesInternalLdapUser.SnAttribute, user.getFamilyName()),
                            new LdapAttribute(CesInternalLdapUser.GivenNameAttribute, user.getGivenName()),
                            new LdapAttribute(CesInternalLdapUser.DisplayNameAttribute, user.getDisplayName()),
                            new LdapAttribute(CesInternalLdapUser.MailAttribute, user.getMail()),
                            new LdapAttribute(CesInternalLdapUser.ExternalAttribute, user.isExternal() ? LDAP_TRUE : LDAP_FALSE)
                    )
                    .build();

            final AddResponse response = modify.execute(request);
            if (!response.isSuccess()) {
                throw new CesLdapException(response.getDiagnosticMessage());
            }

        } catch (LdapException e) {
            throw new CesLdapException("error while creating user", e);
        }
    }

    public void updateUser(CesInternalLdapUser user) throws LdapException {
        final ModifyOperation modify = new ModifyOperation(this.connectionFactory);
        final ModifyRequest request = ModifyRequest.builder()
                .dn(createDnForUser(user))
                .modifications(
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.CnAttribute, user.getUid())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.SnAttribute, user.getFamilyName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.GivenNameAttribute, user.getGivenName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.DisplayNameAttribute, user.getDisplayName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.MailAttribute, user.getMail()))
                        )
                .build();
        final ModifyResponse response = modify.execute(request);

        if (!response.isSuccess()) {
            throw new LdapException(response.getDiagnosticMessage());
        }
    }

    private String createDnForUser(CesInternalLdapUser user) {
        return CesInternalLdapUser.UidAttribute + "=" + user.getUid() + "," + this.baseDN;
    }

    private SearchRequest createGetUserRequest(String uid) {
        String filter = String.format("(&%s%s)", uidFilter(uid), externalUsersFilter());

        SearchRequest request = new SearchRequest();
        request.setBaseDn(this.baseDN);
        request.setReturnAttributes(CesInternalLdapUser.UidAttribute, CesInternalLdapUser.CnAttribute, CesInternalLdapUser.SnAttribute, CesInternalLdapUser.GivenNameAttribute, CesInternalLdapUser.DisplayNameAttribute, CesInternalLdapUser.MailAttribute, CesInternalLdapUser.ExternalAttribute, CesInternalLdapUser.MailAttribute, CesInternalLdapUser.ExternalAttribute, CesInternalLdapUser.MemberOfAttribute);
        request.setFilter(filter);
        request.setSearchScope(SearchScope.SUBTREE);
        request.setSizeLimit(1);
        return request;
    }

    private static String externalUsersFilter() {
        return String.format("(%s=%s)", CesInternalLdapUser.ExternalAttribute, LDAP_TRUE);
    }

    private static String uidFilter(String uid) {
        return String.format("(%s=%s)", CesInternalLdapUser.UidAttribute, uid);
    }
}