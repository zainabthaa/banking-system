package com.zainabtaha.banking.service;

/** Input rules shared by the services. */
final class Validation {

    static final int PIN_LENGTH = 4;
    static final int MIN_ADMIN_PASSWORD_LENGTH = 8;

    private Validation() {
    }

    static void requireValidPin(char[] pin) {
        if (pin == null || pin.length != PIN_LENGTH) {
            throw new IllegalArgumentException("PIN must be exactly " + PIN_LENGTH + " digits.");
        }
        for (char c : pin) {
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("PIN must contain digits only.");
            }
        }
    }

    static void requireStrongAdminPassword(char[] password) {
        if (password == null || password.length < MIN_ADMIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Admin password must be at least " + MIN_ADMIN_PASSWORD_LENGTH + " characters.");
        }
        boolean letter = false;
        boolean digit = false;
        for (char c : password) {
            letter |= Character.isLetter(c);
            digit |= Character.isDigit(c);
        }
        if (!letter || !digit) {
            throw new IllegalArgumentException("Admin password must contain both letters and digits.");
        }
    }
}
