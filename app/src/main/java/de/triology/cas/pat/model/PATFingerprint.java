package de.triology.cas.pat.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable SHA-256 fingerprint of a complete personal access token.
 */
public final class PATFingerprint {
    private static final int SHA_256_BYTES = 32;

    private final byte[] bytes;

    /**
     * Creates a fingerprint from exactly 32 SHA-256 bytes.
     *
     * @param bytes digest bytes
     */
    public PATFingerprint(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (bytes.length != SHA_256_BYTES) {
            throw new IllegalArgumentException("A PAT fingerprint must contain exactly 32 bytes");
        }
        this.bytes = bytes.clone();
    }

    /**
     * Returns a defensive copy for persistence or comparison.
     *
     * @return copied digest bytes
     */
    public byte[] bytes() {
        return bytes.clone();
    }

    /**
     * Provides the equals implementation for this value.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PATFingerprint that && Arrays.equals(bytes, that.bytes);
    }

    /**
     * Returns the value provided by hashCode.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    /**
     * Provides the toString implementation for this value.
     */
    @Override
    public String toString() {
        return "PATFingerprint[******]";
    }
}
