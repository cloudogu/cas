package de.triology.cas.pat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import de.triology.cas.pat.model.CreatePATRequest;
import de.triology.cas.pat.model.PATFingerprint;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.model.StoredPAT;
import de.triology.cas.pat.repository.PATRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PATServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-27T12:00:00Z");
    private static final PATFingerprint FINGERPRINT = new PATFingerprint(new byte[32]);

    @Mock
    private PATRepository repository;
    @Mock
    private SecurePATGenerator generator;

    private PATService service;

    @BeforeEach
    void setUp() {
        service = new PATService(repository, generator, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTokenAndPersistsOnlyFingerprintWithNormalizedMetadata() {
        when(generator.generate()).thenReturn(new GeneratedPAT("pat_secret", FINGERPRINT));
        Instant expiresAt = NOW.plusSeconds(3600);

        var response = service.create(
                " user-1 ", new CreatePATRequest(" My token ", expiresAt, " /api/* "), "usermgt");

        ArgumentCaptor<StoredPAT> storedCaptor = ArgumentCaptor.forClass(StoredPAT.class);
        verify(repository).insert(storedCaptor.capture());
        StoredPAT stored = storedCaptor.getValue();
        assertEquals(response.id(), stored.id());
        assertEquals("user-1", stored.userId());
        assertEquals("My token", stored.displayName());
        assertEquals(FINGERPRINT, stored.tokenFingerprint());
        assertEquals(NOW, stored.createdAt());
        assertEquals(expiresAt, stored.expiresAt());
        assertEquals("/api/*", stored.scope());
        assertEquals("pat_secret", response.token());
        assertEquals(NOW, response.createdAt());
    }

    @Test
    void usesDefaultScopeAndAllowsNoExpiration() {
        when(generator.generate()).thenReturn(new GeneratedPAT("pat_secret", FINGERPRINT));

        var response = service.create("user", new CreatePATRequest("name", null, "  "), "actor");

        assertEquals("/*", response.scope());
        assertNull(response.expiresAt());
    }

    @Test
    void propagatesStorageUnavailableFailure() {
        var expected = new PATStorageUnavailableException(new RuntimeException("offline"));
        when(generator.generate()).thenReturn(new GeneratedPAT("pat_secret", FINGERPRINT));
        org.mockito.Mockito.doThrow(expected).when(repository).insert(any());

        var actual = assertThrows(PATStorageUnavailableException.class,
                () -> service.create("user", new CreatePATRequest("name", null, null), "actor"));

        assertSame(expected, actual);
    }

    @Test
    void rejectsInvalidUserIdsBeforeGeneratingToken() {
        PATRequestException missing = assertThrows(PATRequestException.class,
                () -> service.create(" \t", new CreatePATRequest("name", null, null), "actor"));
        PATRequestException tooLong = assertThrows(PATRequestException.class,
                () -> service.create("x".repeat(256), new CreatePATRequest("name", null, null), "actor"));

        assertEquals("userId is required", missing.getMessage());
        assertEquals("userId must not exceed 255 characters", tooLong.getMessage());
        verify(generator, never()).generate();
    }

    @Test
    void rejectsNonFutureExpirationAndOversizedScope() {
        PATRequestException expiration = assertThrows(PATRequestException.class,
                () -> service.create("user", new CreatePATRequest("name", NOW, null), "actor"));
        PATRequestException scope = assertThrows(PATRequestException.class,
                () -> service.create("user", new CreatePATRequest("name", null, "x".repeat(1001)), "actor"));

        assertEquals("expiresAt must be in the future", expiration.getMessage());
        assertEquals("scope must not exceed 1000 characters", scope.getMessage());
    }

    @Test
    void delegatesOwnerScopedQueries() {
        UUID id = UUID.randomUUID();
        PATMetadata metadata = new PATMetadata(id, "user", "name", NOW, null, "/*");
        when(repository.findAllByUserId("user")).thenReturn(List.of(metadata));
        when(repository.findByUserIdAndId("user", id)).thenReturn(Optional.of(metadata));

        assertEquals(List.of(metadata), service.findAll(" user "));
        assertSame(metadata, service.findOne("user", id));
    }

    @Test
    void treatsAbsentOrForeignTokenAsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findByUserIdAndId("user", id)).thenReturn(Optional.empty());

        PATNotFoundException exception = assertThrows(PATNotFoundException.class,
                () -> service.findOne("user", id));

        assertEquals("PAT not found", exception.getMessage());
    }

    @Test
    void deletesOnlyExistingOwnerScopedToken() {
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(repository.deleteByUserIdAndId("user", existing)).thenReturn(true);
        when(repository.deleteByUserIdAndId("user", missing)).thenReturn(false);

        service.delete(" user ", existing, "actor");
        assertThrows(PATNotFoundException.class, () -> service.delete("user", missing, "actor"));
    }

    @Test
    void resolvesKnownNonExpiredPatByFingerprint() {
        PATFingerprint fingerprint = new PATFingerprint(new byte[32]);
        PATMetadata metadata = new PATMetadata(
                UUID.randomUUID(), "user", "token", NOW.minusSeconds(60), NOW.plusSeconds(60), "/usermgt");
        when(generator.fingerprint("pat_secret")).thenReturn(fingerprint);
        when(repository.validate(fingerprint, NOW)).thenReturn(Optional.of(metadata));

        assertSame(metadata, service.resolve("pat_secret").orElseThrow());
    }

    @Test
    void rejectsMissingAndExpiredPat() {
        assertTrue(service.resolve(null).isEmpty());
        verify(generator, never()).fingerprint(org.mockito.ArgumentMatchers.anyString());

        PATFingerprint fingerprint = new PATFingerprint(new byte[32]);
        PATMetadata expired = new PATMetadata(
                UUID.randomUUID(), "user", "token", NOW.minusSeconds(120), NOW, "/usermgt");
        when(generator.fingerprint("pat_expired")).thenReturn(fingerprint);
        when(repository.validate(fingerprint, NOW)).thenReturn(Optional.of(expired));

        assertTrue(service.resolve("pat_expired").isEmpty());
    }

    @Test
    void matchesScopesAtPathBoundaries() {
        assertTrue(service.isScopeAllowed("/*", "/any/service"));
        assertTrue(service.isScopeAllowed(" /redmine/, /usermgt ", "/usermgt/api/users"));
        assertTrue(service.isScopeAllowed("/usermgt", "/usermgt"));
        assertFalse(service.isScopeAllowed("/user", "/usermgt"));
        assertFalse(service.isScopeAllowed("/usermgt", "/usermgt-admin"));
        assertFalse(service.isScopeAllowed(" ", "/usermgt"));
        assertFalse(service.isScopeAllowed("/usermgt", null));
    }
}
