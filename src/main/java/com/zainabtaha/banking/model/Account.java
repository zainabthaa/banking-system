package com.zainabtaha.banking.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Base class for every bank account.
 *
 * <p>Balance rules are expressed with the <em>template method</em> pattern: {@link #withdraw}
 * is final and asks the subclass how much money is available through {@link #availableFunds()}.
 * That lets {@link CheckingAccount} allow an overdraft without duplicating (or bypassing) the
 * validation logic here.
 *
 * <p>The account only stores a PIN <em>hash</em>; the plain PIN never leaves the authentication
 * layer.
 */
public abstract class Account {

    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("\\d{10}");
    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final String accountNumber;
    private String holderName;
    private BigDecimal balance;
    private String pinHash;
    private int failedLoginAttempts;
    private boolean locked;

    protected Account(String accountNumber, String holderName, BigDecimal balance, String pinHash) {
        this.accountNumber = requireValidNumber(accountNumber);
        this.holderName = requireValidName(holderName);
        this.balance = Money.of(Objects.requireNonNull(balance, "balance"));
        this.pinHash = Objects.requireNonNull(pinHash, "pinHash");
    }

    public abstract AccountType type();

    /** How much can be withdrawn right now. Subclasses override to allow an overdraft. */
    public BigDecimal availableFunds() {
        return balance;
    }

    public final void deposit(BigDecimal amount) {
        balance = balance.add(Money.requirePositive(amount));
    }

    public final void withdraw(BigDecimal amount) {
        BigDecimal value = Money.requirePositive(amount);
        if (value.compareTo(availableFunds()) > 0) {
            throw new InsufficientFundsException(accountNumber, value, availableFunds());
        }
        balance = balance.subtract(value);
    }

    // ----- login lockout state -------------------------------------------------------------

    /** Records a failed PIN attempt and locks the account once the limit is reached. */
    public void registerFailedLogin() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            locked = true;
        }
    }

    public void registerSuccessfulLogin() {
        failedLoginAttempts = 0;
    }

    public void unlock() {
        locked = false;
        failedLoginAttempts = 0;
    }

    public int remainingAttempts() {
        return Math.max(0, MAX_FAILED_ATTEMPTS - failedLoginAttempts);
    }

    // ----- accessors -----------------------------------------------------------------------

    public String accountNumber() {
        return accountNumber;
    }

    public String holderName() {
        return holderName;
    }

    public void rename(String newName) {
        this.holderName = requireValidName(newName);
    }

    public BigDecimal balance() {
        return balance;
    }

    public String pinHash() {
        return pinHash;
    }

    public void changePinHash(String newPinHash) {
        this.pinHash = Objects.requireNonNull(newPinHash, "newPinHash");
    }

    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    /** Used only when rehydrating an account from storage. */
    public void restoreSecurityState(int failedLoginAttempts, boolean locked) {
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException("failedLoginAttempts cannot be negative");
        }
        this.failedLoginAttempts = failedLoginAttempts;
        this.locked = locked;
    }

    // ----- validation ----------------------------------------------------------------------

    public static boolean isValidAccountNumber(String value) {
        return value != null && ACCOUNT_NUMBER.matcher(value).matches();
    }

    private static String requireValidNumber(String value) {
        if (!isValidAccountNumber(value)) {
            throw new IllegalArgumentException("Account number must be exactly 10 digits.");
        }
        return value;
    }

    private static String requireValidName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Account holder name cannot be empty.");
        }
        return value.strip();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account other && accountNumber.equals(other.accountNumber);
    }

    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }

    @Override
    public String toString() {
        return type().label() + " account " + accountNumber + " (" + holderName + ")";
    }
}
