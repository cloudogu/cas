package de.triology.cas.pat.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SecurePATGeneratorTest {

    @Test
    void generatesUrlSafeTokenAndMatchingSha256Fingerprint() throws Exception {
        SecureRandom deterministicRandom = new SecureRandom() {
            @Override
            public void nextBytes(byte[] bytes) {
                Arrays.fill(bytes, (byte) 0xff);
            }
        };

        GeneratedPAT generated = new SecurePATGenerator(deterministicRandom, 32).generate();

        assertTrue(generated.token().startsWith("pat_"));
        assertFalse(generated.token().contains("="));
        assertEquals(47, generated.token().length());
        assertArrayEquals(
                MessageDigest.getInstance("SHA-256").digest(generated.token().getBytes(StandardCharsets.US_ASCII)),
                generated.fingerprint().bytes());
        assertEquals("GeneratedPAT[token=******, fingerprint=******]", generated.toString());
    }

    @org.junit.jupiter.api.Test
    void reportsUnavailableDigestAlgorithmWithOriginalCause() {
        var cause = new java.security.NoSuchAlgorithmException("unavailable");
        try (var digest = org.mockito.Mockito.mockStatic(java.security.MessageDigest.class)) {
            digest.when(() -> java.security.MessageDigest.getInstance("SHA-256")).thenThrow(cause);
            var generator = new SecurePATGenerator(new java.security.SecureRandom(), 32);
            var exception = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> generator.fingerprint("pat_secret"));
            org.junit.jupiter.api.Assertions.assertEquals("SHA-256 is not available", exception.getMessage());
            org.junit.jupiter.api.Assertions.assertSame(cause, exception.getCause());
        }
    }

}
