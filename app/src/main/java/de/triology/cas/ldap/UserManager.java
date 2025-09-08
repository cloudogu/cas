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
        final String dn = createDnForUser(user);

        try {
            // 0) If we plan to set 'external', make sure the entry allows it.
            if (user.isExternal()) {
                ensureAuxObjectClass(dn, "cesperson"); 
            }

            final ModifyOperation modify = operationFactory.modifyOperation();
            final ModifyRequest request = ModifyRequest.builder()
                    .dn(createDnForUser(user))
                    .modifications(
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.CnAttribute, user.getUid())),
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.SnAttribute, user.getFamilyName())),
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.GivenNameAttribute, user.getGivenName())),
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.DisplayNameAttribute, user.getDisplayName())),
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.MailAttribute, user.getMail())),
                            new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(CesInternalLdapUser.ExternalAttribute, user.isExternal() ? LDAP_TRUE : LDAP_FALSE))
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


    /** Ensures the entry has the given auxiliary objectClass (e.g., 'cesperson'). */
    private void ensureAuxObjectClass(String dn, String auxObjectClass) throws CesLdapException {
        try {
            final ModifyOperation mod = operationFactory.modifyOperation();
            final ModifyRequest addOc = ModifyRequest.builder()
                .dn(dn)
                .modifications(new AttributeModification(
                    AttributeModification.Type.ADD,
                    new LdapAttribute(ObjectClassAttributeName, auxObjectClass)))
                .build();

            final ModifyResponse ocResp = mod.execute(addOc);
            if (!ocResp.isSuccess()) {
                // If it already exists, many servers return ATTRIBUTE_OR_VALUE_EXISTS — treat as OK.
                if (!"ATTRIBUTE_OR_VALUE_EXISTS".equalsIgnoreCase(
                        String.valueOf(ocResp.getResultCode()))) {
                    throw new CesLdapException(ocResp.getDiagnosticMessage());
                }
            }
        } catch (LdapException e) {
            throw new CesLdapException("failed to ensure objectClass " + auxObjectClass + " for " + dn, e);
        }
    }

    /**
     * Adds the given user to the given group in LDAP
     *
     * @param user      the user to add
     * @param groupName the name of the group to add the user to
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
        return GroupCnAttribute + "=" + groupName + "," + this.groupBaseDN;
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

    public CesInternalLdapUser getUserByMail(String mail) throws CesLdapException {
        // Search across ALL users (external + internal), because the unique
        // mail constraint applies directory-wide. If you only want external
        // users, call the overloaded method with externalOnly=true.
        return getUserByMail(mail, false);
    }

    public CesInternalLdapUser getUserByMail(String mail, boolean externalOnly) throws CesLdapException {
        final SearchResponse response;
        try {
            final SearchRequest request = createGetUserByMailRequest(mail, externalOnly);
            response = operationFactory.searchOperation().execute(request);
        } catch (final LdapException e) {
            throw new CesLdapException("Failed executing LDAP query", e);
        }

        if (!response.isSuccess()) {
            throw new CesLdapException(response.getDiagnosticMessage());
        }
        if (response.getEntries().size() > 1) {
            throw new CesLdapException("did not expect more then one result for mail=" + mail);
        }

        final LdapEntry entry = response.getEntry();
        return entry == null ? null : CesInternalLdapUser.UserFromEntry(entry);
    }

    private SearchRequest createGetUserByMailRequest(String mail, boolean externalOnly) {
        final String mailFilter = String.format("(%s=%s)", CesInternalLdapUser.MailAttribute, mail);
        final String filter = externalOnly
            ? String.format("(&%s%s)", mailFilter, externalUsersFilter())
            : mailFilter;

        final SearchRequest request = new SearchRequest();
        request.setBaseDn(this.userBaseDN);
        request.setReturnAttributes(
            CesInternalLdapUser.UidAttribute,
            CesInternalLdapUser.CnAttribute,
            CesInternalLdapUser.SnAttribute,
            CesInternalLdapUser.GivenNameAttribute,
            CesInternalLdapUser.DisplayNameAttribute,
            CesInternalLdapUser.MailAttribute,
            CesInternalLdapUser.ExternalAttribute,
            CesInternalLdapUser.MemberOfAttribute
        );
        request.setFilter(filter);
        request.setSearchScope(SearchScope.SUBTREE);
        request.setSizeLimit(1);
        return request;
    }

    public String getUidByMail(String mail) throws CesLdapException {
        final SearchResponse response;
        try {
            final SearchRequest request = new SearchRequest();
            request.setBaseDn(this.userBaseDN);
            request.setFilter(String.format("(%s=%s)", CesInternalLdapUser.MailAttribute, mail));
            request.setReturnAttributes(CesInternalLdapUser.UidAttribute); // only need uid
            request.setSearchScope(SearchScope.SUBTREE);
            request.setSizeLimit(2); // detect duplicates
            response = operationFactory.searchOperation().execute(request);
        } catch (LdapException e) {
            throw new CesLdapException("Failed executing LDAP query", e);
        }

        if (!response.isSuccess()) {
            throw new CesLdapException(response.getDiagnosticMessage());
        }
        if (response.getEntries().size() > 1) {
            throw new CesLdapException("did not expect more then one result for mail=" + mail);
        }

        final LdapEntry entry = response.getEntry();
        if (entry == null) return null;

        final LdapAttribute uidAttr = entry.getAttribute(CesInternalLdapUser.UidAttribute);
        return uidAttr != null ? uidAttr.getStringValue() : null;
    }
}