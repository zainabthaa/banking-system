package com.zainabtaha.banking.model;

import java.math.BigDecimal;
import java.util.Objects;

/** An account that earns interest. Cannot go below zero. */
public final class SavingsAccount extends Account {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Interest rate as a percentage, e.g. {@code 2.50} means 2.5%. */
    private final BigDecimal interestRatePercent;

    public SavingsAccount(String accountNumber, String holderName, BigDecimal balance,
                          String pinHash, BigDecimal interestRatePercent) {
        super(accountNumber, holderName, balance, pinHash);
        Objects.requireNonNull(interestRatePercent, "interestRatePercent");
        if (interestRatePercent.signum() < 0 || interestRatePercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 100 percent.");
        }
        this.interestRatePercent = interestRatePercent;
    }

    @Override
    public AccountType type() {
        return AccountType.SAVINGS;
    }

    public BigDecimal interestRatePercent() {
        return interestRatePercent;
    }

    /** Interest that would be earned for one period on the current balance. */
    public BigDecimal calculateInterest() {
        if (balance().signum() <= 0) {
            return Money.zero();
        }
        return Money.of(balance().multiply(interestRatePercent).divide(HUNDRED, Money.SCALE + 4, Money.ROUNDING));
    }
}
