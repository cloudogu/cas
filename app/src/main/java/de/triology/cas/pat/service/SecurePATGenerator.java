package de.triology.cas.pat.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import de.triology.cas.pat.model.PATFingerprint;

/**
 * Generates high-entropy, URL-safe personal access tokens with the {@code pat_} prefix
 * and their irreversible SHA-256 fingerprints.
 */
public class SecurePATGenerator {
    private static final String TOKEN_PREFIX = "pat_";

    private final SecureRandom secureRandom;
    private final int randomBytes;

    /**
     * Creates a token generator.
     *
     * @param secureRandom cryptographically secure random source
     * @param randomBytes number of random bytes per token
     */
    public SecurePATGenerator(SecureRandom secureRandom, int randomBytes) {
        this.secureRandom = secureRandom;
        this.randomBytes = randomBytes;
    }

    /**
     * Generates a token consisting of the {@code pat_} prefix and a Base64-URL encoded
     * random value without padding, together with the SHA-256 fingerprint stored by the service.
     *
     * @return newly generated token and fingerprint
     */
    public GeneratedPAT generate() {
        byte[] random = new byte[randomBytes];
        secureRandom.nextBytes(random);
        String token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return new GeneratedPAT(token, fingerprint(token));
    }

    /**
     * Computes the irreversible fingerprint of the complete cleartext token.
     *
     * @param token complete cleartext token including its prefix
     * @return SHA-256 digest
     */
    public PATFingerprint fingerprint(String token) {
        try {
            return new PATFingerprint(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
