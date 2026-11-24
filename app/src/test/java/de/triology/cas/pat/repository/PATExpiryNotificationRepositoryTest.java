package de.triology.cas.pat.repository;

import static org.junit.jupiter.api.Assertions.*;
import de.triology.cas.pat.model.PATFingerprint;
import de.triology.cas.pat.model.StoredPAT;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

class PATExpiryNotificationRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-11-17T07:00:00Z");
    @TempDir Path directory;
    private JdbcTemplate jdbc;
    private JdbcPATRepository pats;
    private PATExpiryNotificationRepository repository;

    @BeforeEach
    void setUp() {
        var config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        var dataSource = new SQLiteDataSource(config);
        dataSource.setUrl("jdbc:sqlite:" + directory.resolve("pat.db"));
        Flyway.configure().dataSource(dataSource).locations("classpath:db/pat/migration/sqlite").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        pats = new JdbcPATRepository(jdbc);
        repository = new PATExpiryNotificationRepository(jdbc);
    }

    private StoredPAT insert(Instant expiresAt, String owner, boolean notify) {
        var pat = new StoredPAT(UUID.randomUUID(), "user", "name", new PATFingerprint(new byte[32]),
                NOW, expiresAt, "/*", "", owner, notify);
        pats.insert(pat);
        return pat;
    }

    @Test
    void selectsOnlyEnabledUnexpiredPatsWithOwners() {
        var candidate = insert(NOW.plusSeconds(3600), "ldap-owner", true);
        insert(NOW.plusSeconds(3600), "ldap-owner", false);
        insert(NOW, "ldap-owner", true);
        insert(NOW.minusSeconds(1), "ldap-owner", true);
        insert(null, "ldap-owner", true);
        insert(NOW.plusSeconds(3600), "  ", true);
        insert(NOW.plusSeconds(3600), null, true);

        assertEquals(java.util.List.of(candidate.id()), repository.findCandidates(NOW).stream()
                .map(metadata -> metadata.id()).toList());
    }

    @Test
    void remembersSuccessfulStagesAcrossRepositoryInstancesAndCascadesDeletion() {
        var stored = insert(NOW.plusSeconds(86400), "ldap-owner", true);
        var pat = pats.findByUserIdAndId("user", stored.id()).orElseThrow();
        var sent = new AtomicInteger();
        assertTrue(repository.sendOnce(pat, 1, NOW, sent::incrementAndGet));
        assertFalse(new PATExpiryNotificationRepository(jdbc).sendOnce(pat, 1, NOW, sent::incrementAndGet));
        assertEquals(1, sent.get());
        pats.deleteByUserIdAndId("user", pat.id());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pat_expiry_notifications", Integer.class));
    }

    @Test
    void failedSendsRollBackTheirStageAndCanBeRetried() {
        var stored = insert(NOW.plusSeconds(86400), "ldap-owner", true);
        var pat = pats.findByUserIdAndId("user", stored.id()).orElseThrow();
        assertThrows(IllegalStateException.class, () -> repository.sendOnce(pat, 1, NOW, () -> {
            throw new IllegalStateException("SMTP unavailable");
        }));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM pat_expiry_notifications", Integer.class));
        assertTrue(repository.sendOnce(pat, 1, NOW, () -> {}));
    }
}
