package com.example.demo.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHashUtilsTest {

    @Test
    void encodedPasswordMatchesOnlyOriginalPassword() {
        String encoded = PasswordHashUtils.encode("test-password");

        assertTrue(PasswordHashUtils.matches("test-password", encoded));
        assertFalse(PasswordHashUtils.matches("different-password", encoded));
    }

    @Test
    void malformedEncodedPasswordDoesNotMatch() {
        assertFalse(PasswordHashUtils.matches("test-password", "not-a-password-hash"));
    }
}
