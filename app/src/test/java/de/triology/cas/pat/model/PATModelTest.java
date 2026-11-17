package de.triology.cas.pat.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PATModelTest {

    @Test
    void fingerprintIsValidatedImmutableAndValueBased() {
        byte[] source = new byte[32];
        source[0] = 7;
        PATFingerprint fingerprint = new PATFingerprint(source);
        source[0] = 9;
        byte[] returned = fingerprint.bytes();
        returned[0] = 11;

        assertArrayEquals(new byte[] {7}, new byte[] {fingerprint.bytes()[0]});
        assertEquals(new PATFingerprint(fingerprint.bytes()), fingerprint);
        assertEquals(new PATFingerprint(fingerprint.bytes()).hashCode(), fingerprint.hashCode());
        assertNotEquals(new PATFingerprint(new byte[32]), fingerprint);
        assertNotEquals("fingerprint", fingerprint);
        assertEquals("PATFingerprint[******]", fingerprint.toString());
        assertThrows(NullPointerException.class, () -> new PATFingerprint(null));
        assertThrows(IllegalArgumentException.class, () -> new PATFingerprint(new byte[31]));
    }

    @Test
    void secretBearingModelsMaskTheirSecrets() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-08-27T12:00:00Z");
        String token = "pat_should_never_be_logged";
        CreatePATResponse response = new CreatePATResponse(id, "user", "name", token, now, null, "/*");
        StoredPAT stored = new StoredPAT(id, "user", "name", new PATFingerprint(new byte[32]), now, null, "/*");

        assertFalse(response.toString().contains(token));
        assertFalse(stored.toString().contains(new String(new byte[32])));
        assertTrueMasked(response.toString());
        assertTrueMasked(stored.toString());
    }

    @Test
    void acceptsOptionalJsonParametersIncludingFalse() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var request = mapper.readValue("""
                {"displayName":"name", "description":"Beschreibung", "owner":"ldap-user",
                 "expiryNotification":false}
                """, CreatePATRequest.class);

        assertEquals("Beschreibung", request.description());
        assertEquals("ldap-user", request.owner());
        assertEquals(Boolean.FALSE, request.expiryNotification());
    }

    @Test
    void acceptsMissingAndNullOptionalJsonParameters() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        for (String json : new String[] {
                "{\"displayName\":\"name\"}",
                """
                {"displayName":"name", "description":null, "owner":null, "expiryNotification":null}
                """}) {
            var request = mapper.readValue(json, CreatePATRequest.class);
            assertNull(request.description());
            assertNull(request.owner());
            assertNull(request.expiryNotification());
        }
    }

    @Test
    void createRequestRejectsUnknownFields() {
        CreatePATRequest request = new CreatePATRequest("name", null, null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> request.rejectUnknownField("unexpected", 42));

        assertEquals("Unknown field: unexpected", exception.getMessage());
    }

    @Test
    void errorFactoryKeepsStableContract() {
        Instant timestamp = Instant.parse("2026-08-27T12:00:00Z");
        assertEquals(new PATErrorResponse("CODE", "message", timestamp),
                PATErrorResponse.of("CODE", "message", timestamp));
    }

    private static void assertTrueMasked(String value) {
        org.junit.jupiter.api.Assertions.assertTrue(value.contains("******"));
    }
}
