package de.triology.cas.pat.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.triology.cas.pat.model.PATFingerprint;
import de.triology.cas.pat.model.StoredPAT;
import de.triology.cas.pat.service.PATStorageUnavailableException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.sqlite.SQLiteDataSource;

@ExtendWith(MockitoExtension.class)
class JdbcPATRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");

    @TempDir
    java.nio.file.Path tempDir;
    @Mock
    private JdbcTemplate failingTemplate;

    private JdbcTemplate jdbcTemplate;
    private JdbcPATRepository repository;

    @BeforeEach
    void setUp() {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("pat.db"));
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/pat/migration/sqlite")
                .load()
                .migrate();
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcPATRepository(jdbcTemplate);
    }

    @Test
    void storesMapsOrdersAndDeletesOwnerScopedMetadata() {
        StoredPAT older = pat(UUID.randomUUID(), "owner", "older", NOW, null);
        StoredPAT newer = pat(UUID.randomUUID(), "owner", "newer", NOW.plusSeconds(60), NOW.plusSeconds(3600));
        StoredPAT foreign = pat(UUID.randomUUID(), "other", "foreign", NOW.plusSeconds(120), null);

        repository.insert(older);
        repository.insert(newer);
        repository.insert(foreign);

        var all = repository.findAllByUserId("owner");
        assertEquals(List.of(newer.id(), older.id()), all.stream().map(metadata -> metadata.id()).toList());
        assertEquals(newer.expiresAt(), all.getFirst().expiresAt());
        assertEquals("/*", all.getFirst().scope());
        assertTrue(repository.findByUserIdAndId("owner", newer.id()).isPresent());
        assertTrue(repository.findByUserIdAndId("other", newer.id()).isEmpty());
        assertFalse(repository.deleteByUserIdAndId("other", newer.id()));
        assertTrue(repository.deleteByUserIdAndId("owner", newer.id()));
        assertTrue(repository.findByUserIdAndId("owner", newer.id()).isEmpty());
    }

    @Test
    void neverStoresTheCleartextToken() {
        StoredPAT pat = pat(UUID.randomUUID(), "owner", "name", NOW, null);
        repository.insert(pat);

        Integer fingerprintLength = jdbcTemplate.queryForObject(
                "SELECT length(token_fingerprint) FROM personal_access_tokens WHERE id = ?",
                Integer.class,
                pat.id().toString());
        assertEquals(32, fingerprintLength);
    }

    @Test
    void reportsInvalidStoredMetadataAsIntegrityViolation() {
        jdbcTemplate.update("""
                INSERT INTO personal_access_tokens
                    (id, user_id, display_name, token_fingerprint, created_at, expires_at, scope)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "not-a-uuid", "owner", "name", new byte[32], NOW.toString(), null, "/*");

        assertThrows(DataIntegrityViolationException.class, () -> repository.findAllByUserId("owner"));
    }

    @Test
    void translatesResourceFailuresAtRepositoryBoundary() {
        DataAccessResourceFailureException cause = new DataAccessResourceFailureException("offline");
        when(failingTemplate.update(any(PreparedStatementCreator.class))).thenThrow(cause);
        JdbcPATRepository failingRepository = new JdbcPATRepository(failingTemplate);

        PATStorageUnavailableException exception = assertThrows(PATStorageUnavailableException.class,
                () -> failingRepository.insert(pat(UUID.randomUUID(), "owner", "name", NOW, null)));

        assertEquals("PAT storage is unavailable", exception.getMessage());
        assertInstanceOf(DataAccessResourceFailureException.class, exception.getCause());
    }

    @Test
    void leavesNonAvailabilityDataFailuresUnchanged() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("duplicate");
        when(failingTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenThrow(cause);
        JdbcPATRepository failingRepository = new JdbcPATRepository(failingTemplate);

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> failingRepository.findAllByUserId("owner"));

        assertEquals(cause, exception);
    }

    private static StoredPAT pat(UUID id, String userId, String name, Instant createdAt, Instant expiresAt) {
        byte[] fingerprint = new byte[32];
        fingerprint[0] = (byte) id.hashCode();
        return new StoredPAT(id, userId, name, new PATFingerprint(fingerprint), createdAt, expiresAt, "/*");
    }
}
