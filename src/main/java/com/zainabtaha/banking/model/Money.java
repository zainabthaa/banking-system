package com.zainabtaha.banking.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helpers for monetary values.
 *
 * <p>All money in the system is a {@link BigDecimal} with exactly two decimal places.
 * {@code double} is never used for money because binary floating point cannot represent
 * values like 0.10 exactly, which causes rounding drift over many transactions.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {
    }

    /** Normalises a value to two decimal places using banker's rounding. */
    public static BigDecimal of(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal of(String value) {
        return of(new BigDecimal(value));
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    /**
     * Validates that an amount is strictly positive and has no more than two decimal places.
     *
     * @throws IllegalArgumentException if the amount is null, zero, negative or too precise
     */
    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
        return of(amount);
    }
}
