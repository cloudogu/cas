package de.triology.cas.ldap;

import lombok.extern.slf4j.Slf4j;
import org.ldaptive.*;

/**
 * UserManager can load, create and update {@link CesInternalLdapUser}s.
 */
@Slf4j
public class UserManager {
    public static final String LDAP_TRUE = "TRUE";
    public static final String LDAP_FALSE = "FALSE";

    public static final String GroupOu = "Groups";
    public static final String GroupCnAttribute = "cn";
    public static final String GroupMemberAttribute = "member";

    public static final String ObjectClassAttributeName = "objectClass";

    private final String userBaseDN;
    private final String groupBaseDN;
    private final LdapOperationFactory operationFactory;

    /**
     * Creates a new Usermanager that can load, create and update {@link CesInternalLdapUser}s.
     */
    public UserManager(String userBaseDN, LdapOperationFactory operationFactory) {
        this.userBaseDN = userBaseDN;
        this.groupBaseDN = createGroupBaseDNFromBaseDN(userBaseDN);
        this.operationFactory = operationFactory;
    }

    private static String createGroupBaseDNFromBaseDN(String userBaseDN) {
        return userBaseDN.replaceFirst("(?<=ou=)[^,]+(?=,)", GroupOu);
    }

    /**
     * Gets the {@link CesInternalLdapUser} for the given UID.
     *
     * @param uid the UID of the user to get
     * @return null if the user for the given UID could not be found
     * @throws CesLdapException for errors querying LDAP
     */
    public CesInternalLdapUser getUserByUid(String uid) throws CesLdapException {
        final SearchResponse response;
        try {
            final SearchRequest request = createGetUserRequest(uid);
            response = operationFactory.searchOperation().execute(request);
        } catch (final LdapException e) {
            throw new CesLdapException("Failed executing LDAP query", e);
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


    /**
     * Creates a new {@link CesInternalLdapUser}
     *
     * @param user the user to create
     * @throws CesLdapException for errors while creating the user in LDAP
     */
    public void createUser(CesInternalLdapUser user) throws CesLdapException {
        try {
            final AddOperation modify = operationFactory.addOperation();
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

    /**
     * Updates the given user in LDAP
     *
     * @param user the user to update
     * @throws CesLdapException for errors while updating the user in LDAP
     */
    public void updateUser(CesInternalLdapUser user) throws CesLdapException {
        try {
            final ModifyOperation modify = operationFactory.modifyOperation();
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
                throw new CesLdapException(response.getDiagnosticMessage());
            }
        } catch (LdapException e) {
            throw new CesLdapException("error while updating user", e);
        }
    }

    /**
     * Adds the given user to the given group in LDAP
     *
     * @param user      the user to add
     * @param groupName the name of the group to add the use to
     * @throws CesLdapException for errors in LDAP
     */
    public void addUserToGroup(CesInternalLdapUser user, String groupName) throws CesLdapException {
        try {
            final ModifyOperation modify = operationFactory.modifyOperation();
            ModifyRequest request = new ModifyRequest(
                    createDnForGroup(groupName),
                    new AttributeModification(AttributeModification.Type.ADD, new LdapAttribute(GroupMemberAttribute, createDnForUser(user)))
            );
            final ModifyResponse response = modify.execute(request);

            if (!response.isSuccess()) {
                throw new CesLdapException(response.getDiagnosticMessage());
            }
        } catch (LdapException e) {
            throw new CesLdapException("error while adding user to group", e);
        }
    }

    private String createDnForUser(CesInternalLdapUser user) {
        return CesInternalLdapUser.UidAttribute + "=" + user.getUid() + "," + this.userBaseDN;
    }

    private String createDnForGroup(String groupName) {
        return CesInternalLdapUser.CnAttribute + "=" + groupName + "," + this.groupBaseDN;
    }

    private SearchRequest createGetUserRequest(String uid) {
        String filter = String.format("(&%s%s)", uidFilter(uid), externalUsersFilter());

        SearchRequest request = new SearchRequest();
        request.setBaseDn(this.userBaseDN);
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