package de.triology.cas.sync;

import de.triology.cas.ldap.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs the OIDC to LDAP synchronization on the configured schedule. */
@Slf4j
public class ScheduledOidcLdapSynchronizer {
    private final OidcLdapSynchronizer synchronizer;

    /** Creates a scheduled synchronizer using the configured SCIM and LDAP clients. */
    public ScheduledOidcLdapSynchronizer(ScimClient scimClient, UserManager userManager,
                                         OidcLdapSyncProperties properties) {
        this.synchronizer = new OidcLdapSynchronizer(scimClient, userManager, properties);
    }

    /** Runs one synchronization and logs failures without stopping future scheduled runs. */
    @Scheduled(initialDelay = 0, fixedDelayString = "${oidc.ldap-sync.interval}")
    public void run() {
        try {
            synchronizer.synchronize();
        } catch (Exception e) {
            LOGGER.error("OIDC/LDAP synchronization failed; no further users will be deleted in this run", e);
        }
    }
}
