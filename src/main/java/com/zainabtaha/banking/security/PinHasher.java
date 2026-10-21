package com.zainabtaha.banking.security;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes and verifies PINs and passwords with PBKDF2-HMAC-SHA256 and a random per-secret salt.
 *
 * <p>Hashes are stored in a self-describing format so the work factor can be raised later
 * without breaking existing records:
 * <pre>pbkdf2-sha256$&lt;iterations&gt;$&lt;base64 salt&gt;$&lt;base64 hash&gt;</pre>
 *
 * <p>Secrets are accepted as {@code char[]} (as returned by {@code JPasswordField}) so callers
 * can wipe them from memory after use, and comparison is constant-time to avoid timing leaks.
 */
public final class PinHasher {

    /** OWASP 2023 recommendation for PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public PinHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /** Lower iteration counts are useful in unit tests; production code uses the default. */
    public PinHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    public String hash(char[] secret) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = pbkdf2(secret, salt, iterations);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + iterations + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public boolean verify(char[] secret, String stored) {
        if (secret == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int storedIterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(secret, salt, storedIterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false; // malformed record
        }
    }

    /** Convenience for call sites that only have a String (e.g. seeding demo data). */
    public String hash(String secret) {
        char[] chars = secret.toCharArray();
        try {
            return hash(chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    private static byte[] pbkdf2(char[] secret, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available on this JVM", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Overwrites a secret in memory. Safe to call with null. */
    public static void wipe(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
