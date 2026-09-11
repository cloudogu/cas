package de.triology.cas.pat.service;

import de.triology.cas.pat.model.PATFingerprint;

/**
 * Newly generated cleartext PAT together with its irreversible persistence fingerprint.
 *
 * @param token URL-safe cleartext token beginning with {@code pat_}
 * @param fingerprint SHA-256 fingerprint of the token
 */
public record GeneratedPAT(String token, PATFingerprint fingerprint) {
    /**
     * Returns a log-safe representation that masks secret material.
     *
     * @return masked value description
     */
    @Override
    public String toString() {
        return "GeneratedPAT[token=******, fingerprint=******]";
    }
}
