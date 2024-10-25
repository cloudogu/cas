package de.triology.cas.ldap;

import lombok.extern.slf4j.Slf4j;
import org.ldaptive.*;

@Slf4j
public class UserManager {
    public static final String LDAP_TRUE = "TRUE";
    public static final String LDAP_FALSE = "FALSE";

    private final String baseDN;
    private final ConnectionFactory connectionFactory;


    public UserManager(String baseDN, ConnectionFactory connectionFactory) {
        this.baseDN = baseDN;
        this.connectionFactory = connectionFactory;
    }

    public CesLdapUser getUserByUid(String uid) throws CesLdapException {
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

        return CesLdapUser.UserFromEntry(entry);
    }


    public void createUser(CesLdapUser user) throws CesLdapException {
        try {
            final AddOperation modify = new AddOperation(this.connectionFactory);
            final AddRequest request = AddRequest.builder()
                    .dn(createDnForUser(user))
                    .attributes(
                            //Fixme mapping for objectlasses
                            new LdapAttribute("objectClass", "top", "person", "inetOrgPerson", "organizationalPerson", "cesperson"),
                            new LdapAttribute(CesLdapUser.CnAttribute, user.getUid()),
                            new LdapAttribute(CesLdapUser.SnAttribute, user.getFamilyName()),
                            new LdapAttribute(CesLdapUser.GivenNameAttribute, user.getGivenName()),
                            new LdapAttribute(CesLdapUser.DisplayNameAttribute, user.getDisplayName()),
                            new LdapAttribute(CesLdapUser.MailAttribute, user.getMail()),
                            new LdapAttribute(CesLdapUser.ExternalAttribute, user.isExternal() ? LDAP_TRUE : LDAP_FALSE)
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

    public void updateUser(CesLdapUser user) throws LdapException {
        final ModifyOperation modify = new ModifyOperation(this.connectionFactory);
        final ModifyRequest request = ModifyRequest.builder()
                .dn(createDnForUser(user))
                .modifications(
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesLdapUser.CnAttribute, user.getUid())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesLdapUser.SnAttribute, user.getFamilyName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesLdapUser.GivenNameAttribute, user.getGivenName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesLdapUser.DisplayNameAttribute, user.getDisplayName())),
                        new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesLdapUser.MailAttribute, user.getMail()))
                        )
                .build();
        final ModifyResponse response = modify.execute(request);

        if (!response.isSuccess()) {
            throw new LdapException(response.getDiagnosticMessage());
        }
    }

    private String createDnForUser(CesLdapUser user) {
        return CesLdapUser.UidAttribute + "=" + user.getUid() + "," + this.baseDN;
    }

    private SearchRequest createGetUserRequest(String uid) {
        SearchRequest request = new SearchRequest();
        request.setBaseDn(this.baseDN);
        request.setReturnAttributes(CesLdapUser.UidAttribute, CesLdapUser.CnAttribute, CesLdapUser.SnAttribute, CesLdapUser.GivenNameAttribute, CesLdapUser.DisplayNameAttribute, CesLdapUser.MailAttribute, CesLdapUser.ExternalAttribute, CesLdapUser.MailAttribute, CesLdapUser.ExternalAttribute, CesLdapUser.MemberOfAttribute);
        //Fixme
        request.setFilter(String.format("(&(objectClass=person)(%s=TRUE)(%s=%s))", CesLdapUser.ExternalAttribute, CesLdapUser.UidAttribute, uid));
        request.setSearchScope(SearchScope.SUBTREE);
        request.setSizeLimit(1);
        return request;
    }

}