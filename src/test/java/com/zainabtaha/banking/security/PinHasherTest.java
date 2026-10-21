package com.zainabtaha.banking.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PinHasherTest {

    private final PinHasher hasher = new PinHasher(1_000);

    @Test
    void verifiesCorrectPin() {
        String hash = hasher.hash("1234".toCharArray());
        assertTrue(hasher.verify("1234".toCharArray(), hash));
    }

    @Test
    void rejectsWrongPin() {
        String hash = hasher.hash("1234".toCharArray());
        assertFalse(hasher.verify("1235".toCharArray(), hash));
    }

    @Test
    void neverStoresThePlainPin() {
        String hash = hasher.hash("1234".toCharArray());
        assertFalse(hash.contains("1234"));
        assertTrue(hash.startsWith("pbkdf2-sha256$1000$"));
    }

    @Test
    void samePinGetsDifferentSalt() {
        assertNotEquals(hasher.hash("1234".toCharArray()), hasher.hash("1234".toCharArray()));
    }

    @Test
    void verifiesHashesCreatedWithADifferentWorkFactor() {
        String oldHash = new PinHasher(500).hash("9999".toCharArray());
        assertTrue(hasher.verify("9999".toCharArray(), oldHash));
    }

    @Test
    void malformedHashesNeverVerify() {
        assertFalse(hasher.verify("1234".toCharArray(), "1234"));
        assertFalse(hasher.verify("1234".toCharArray(), "pbkdf2-sha256$x$y$z"));
        assertFalse(hasher.verify("1234".toCharArray(), null));
        assertFalse(hasher.verify(null, hasher.hash("1234".toCharArray())));
    }
}
