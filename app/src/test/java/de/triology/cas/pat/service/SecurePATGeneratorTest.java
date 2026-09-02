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
}
