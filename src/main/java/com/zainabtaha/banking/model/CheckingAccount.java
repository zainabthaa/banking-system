package com.zainabtaha.banking.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An everyday account with overdraft protection: the balance may go negative,
 * down to {@code -overdraftLimit}.
 */
public final class CheckingAccount extends Account {

    private final BigDecimal overdraftLimit;

    public CheckingAccount(String accountNumber, String holderName, BigDecimal balance,
                           String pinHash, BigDecimal overdraftLimit) {
        super(accountNumber, holderName, balance, pinHash);
        Objects.requireNonNull(overdraftLimit, "overdraftLimit");
        if (overdraftLimit.signum() < 0) {
            throw new IllegalArgumentException("Overdraft limit cannot be negative.");
        }
        this.overdraftLimit = Money.of(overdraftLimit);
    }

    @Override
    public AccountType type() {
        return AccountType.CHECKING;
    }

    @Override
    public BigDecimal availableFunds() {
        return balance().add(overdraftLimit);
    }

    public BigDecimal overdraftLimit() {
        return overdraftLimit;
    }

    public boolean isOverdrawn() {
        return balance().signum() < 0;
    }
}
