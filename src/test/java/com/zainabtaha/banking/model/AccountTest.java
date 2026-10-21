package com.zainabtaha.banking.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountTest {

    private static final String HASH = "hash";

    private static SavingsAccount savings(String balance) {
        return new SavingsAccount("1234567890", "Test User", new BigDecimal(balance), HASH, new BigDecimal("2.5"));
    }

    private static CheckingAccount checking(String balance, String overdraft) {
        return new CheckingAccount("1234567890", "Test User", new BigDecimal(balance), HASH, new BigDecimal(overdraft));
    }

    @Test
    void depositIncreasesBalance() {
        SavingsAccount account = savings("100.00");
        account.deposit(new BigDecimal("50.25"));
        assertEquals(new BigDecimal("150.25"), account.balance());
    }

    @Test
    void moneyArithmeticIsExact() {
        // With double, 0.1 + 0.2 != 0.3. BigDecimal keeps cents exact.
        SavingsAccount account = savings("0.00");
        account.deposit(new BigDecimal("0.10"));
        account.deposit(new BigDecimal("0.20"));
        assertEquals(new BigDecimal("0.30"), account.balance());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01", "10.001"})
    void rejectsInvalidAmounts(String amount) {
        SavingsAccount account = savings("100.00");
        assertThrows(IllegalArgumentException.class, () -> account.deposit(new BigDecimal(amount)));
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(new BigDecimal(amount)));
        assertEquals(new BigDecimal("100.00"), account.balance());
    }

    @Test
    void savingsCannotGoBelowZero() {
        SavingsAccount account = savings("100.00");
        InsufficientFundsException e = assertThrows(InsufficientFundsException.class,
                () -> account.withdraw(new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), e.available());
        assertEquals(new BigDecimal("100.00"), account.balance());
    }

    @Test
    void checkingCanUseOverdraft() {
        // This was broken in the original version: the parent withdraw() rejected anything over the balance.
        CheckingAccount account = checking("100.00", "500.00");
        account.withdraw(new BigDecimal("450.00"));
        assertEquals(new BigDecimal("-350.00"), account.balance());
        assertTrue(account.isOverdrawn());
        assertEquals(new BigDecimal("150.00"), account.availableFunds());
    }

    @Test
    void checkingCannotExceedOverdraftLimit() {
        CheckingAccount account = checking("100.00", "500.00");
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(new BigDecimal("600.01")));
        account.withdraw(new BigDecimal("600.00"));
        assertEquals(new BigDecimal("-500.00"), account.balance());
    }

    @Test
    void savingsInterestIsRoundedToCents() {
        SavingsAccount account = savings("1234.56");
        // 1234.56 * 2.5% = 30.864 -> 30.86
        assertEquals(new BigDecimal("30.86"), account.calculateInterest());
    }

    @Test
    void locksAfterThreeFailedLogins() {
        SavingsAccount account = savings("0.00");
        account.registerFailedLogin();
        account.registerFailedLogin();
        assertFalse(account.isLocked());
        assertEquals(1, account.remainingAttempts());
        account.registerFailedLogin();
        assertTrue(account.isLocked());

        account.unlock();
        assertFalse(account.isLocked());
        assertEquals(3, account.remainingAttempts());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345678901", "12345abcde", " 123456789"})
    void rejectsInvalidAccountNumbers(String number) {
        assertThrows(IllegalArgumentException.class,
                () -> new SavingsAccount(number, "Name", BigDecimal.ZERO, HASH, BigDecimal.ONE));
    }

    @Test
    void rejectsBlankHolderName() {
        assertThrows(IllegalArgumentException.class,
                () -> new SavingsAccount("1234567890", "   ", BigDecimal.ZERO, HASH, BigDecimal.ONE));
    }

    @Test
    void rejectsNegativeOverdraftLimit() {
        assertThrows(IllegalArgumentException.class, () -> checking("0", "-1"));
    }
}
