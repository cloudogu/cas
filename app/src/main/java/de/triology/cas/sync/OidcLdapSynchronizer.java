package de.triology.cas.sync;

import de.triology.cas.ldap.CesLdapException;
import de.triology.cas.ldap.LdapUserReference;
import de.triology.cas.ldap.UserManager;
import de.triology.cas.pat.service.PATOperationException;
import de.triology.cas.pat.service.PATService;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Removes LDAP users managed by OIDC when they are no longer in configured SCIM groups. */
@Slf4j
public class OidcLdapSynchronizer {
    private final ScimClient scimClient;
    private final UserManager userManager;
    private final OidcLdapSyncProperties properties;
    private final PATService patService;

    /** Creates a synchronizer with the SCIM client, LDAP manager, and runtime settings. */
    public OidcLdapSynchronizer(ScimClient scimClient, UserManager userManager,
                                OidcLdapSyncProperties properties, PATService patService) {
        this.scimClient = scimClient;
        this.userManager = userManager;
        this.properties = properties;
        this.patService = patService;
    }

    /** Loads allowed identities and deletes managed LDAP users absent from those identities. */
    public void synchronize() throws CesLdapException {
        if (properties.getScim().getGroupIds().isEmpty()) {
            throw new IllegalStateException("At least one OIDC group must be configured");
        }

        long deadline = System.nanoTime() + properties.getTimeout().toNanos();
        Map<String, String> allowedIdentities = new HashMap<>();
        for (String groupId : properties.getScim().getGroupIds()) {
            if (groupId == null || groupId.isBlank()) {
                throw new IllegalStateException("OIDC group IDs must not be blank");
            }
            for (ScimClient.GroupMember member : scimClient.getGroupMembers(groupId, deadline)) {
                allowedIdentities.putIfAbsent(member.identity(), member.id());
            }
        }

        ensureWithinDeadline(deadline);
        List<LdapUserReference> ldapUsers = userManager.getExternalUsers(
                properties.getLdap().getExternalAttribute(),
                properties.getLdap().getExternalValue(),
                properties.getMapping().getLdapIdentityAttribute(),
                deadline);

        int deleted = 0;
        for (LdapUserReference user : ldapUsers) {
            ensureWithinDeadline(deadline);
            if (!allowedIdentities.containsKey(user.identity())) {
                userManager.deleteUser(user.dn());
                deleted++;
                LOGGER.warn("{}", deletionAuditMessage(user.identity(), user.dn(), Instant.now()));

                if (patService != null) {
                    try {
                        patService.deleteAllForUser(user.identity());
                    } catch (PATOperationException e) {
                        LOGGER.warn("Failed to remove PATs for deleted federated user {}", user.dn());
                    }
                }
            }
        }
        LOGGER.info("OIDC/LDAP synchronization completed: allowedGroupMembers={}, ldapUsers={}, deleted={}",
                allowedIdentities.size(), ldapUsers.size(), deleted);
    }

    /** Fails the current run when its deadline has elapsed. */
    private static void ensureWithinDeadline(long deadlineNanos) {
        if (System.nanoTime() >= deadlineNanos) {
            throw new IllegalStateException("OIDC/LDAP synchronization timeout exceeded");
        }
    }

    static String deletionAuditMessage(String identity, String dn, Instant deletedAt) {
        return "Deleted federated LDAP user scimId=not-present identity=" + identity
                + " dn=" + dn + " deletedAt=" + deletedAt
                + " reason=identity is not present in a configured OIDC group";
    }
}
