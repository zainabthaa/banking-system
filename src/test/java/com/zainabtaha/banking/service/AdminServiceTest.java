package com.zainabtaha.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zainabtaha.banking.TestFixtures;
import com.zainabtaha.banking.TestFixtures.InMemoryAccounts;
import com.zainabtaha.banking.TestFixtures.InMemoryTransactions;
import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.model.SavingsAccount;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.Exceptions.AccountNotFoundException;
import com.zainabtaha.banking.service.Exceptions.DuplicateAccountException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminServiceTest {

    private InMemoryAccounts accounts;
    private InMemoryTransactions transactions;
    private PinHasher hasher;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        transactions = new InMemoryTransactions();
        hasher = TestFixtures.fastHasher();
        admin = new AdminService(accounts, transactions, hasher, TestFixtures.CLOCK, new Random(42));
    }

    private Account open(String number, AccountType type, String deposit) {
        return admin.openAccount(new OpenAccountRequest(number, "Holder " + number, "1234".toCharArray(),
                type, new BigDecimal(deposit), new BigDecimal("5")));
    }

    @Test
    void openAccountHashesPinAndRecordsOpeningDeposit() {
        Account account = open("1234567890", AccountType.SAVINGS, "250.00");

        assertTrue(hasher.verify("1234".toCharArray(), account.pinHash()));
        assertFalse(account.pinHash().contains("1234"));
        List<Transaction> ledger = transactions.findByAccount("1234567890");
        assertEquals(1, ledger.size());
        assertEquals(TransactionType.OPENING_DEPOSIT, ledger.get(0).type());
        assertEquals(new BigDecimal("250.00"), account.balance());
    }

    @Test
    void zeroOpeningDepositCreatesNoTransaction() {
        open("1234567890", AccountType.CHECKING, "0");
        assertTrue(transactions.findAll().isEmpty());
    }

    @Test
    void duplicateAccountNumbersAreRejected() {
        // The original "Create User" screen appended duplicates to users.txt without checking.
        open("1234567890", AccountType.SAVINGS, "0");
        assertThrows(DuplicateAccountException.class, () -> open("1234567890", AccountType.CHECKING, "0"));
    }

    @Test
    void invalidPinIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> admin.openAccount(new OpenAccountRequest(
                "1234567890", "Name", "12a4".toCharArray(), AccountType.SAVINGS, BigDecimal.ZERO, BigDecimal.ONE)));
        assertThrows(IllegalArgumentException.class, () -> admin.openAccount(new OpenAccountRequest(
                "1234567890", "Name", "123".toCharArray(), AccountType.SAVINGS, BigDecimal.ZERO, BigDecimal.ONE)));
        assertTrue(accounts.findAll().isEmpty());
    }

    @Test
    void generatedAccountNumbersAreValidAndUnused() {
        String first = admin.generateAccountNumber();
        assertTrue(Account.isValidAccountNumber(first));
        open(first, AccountType.SAVINGS, "0");
        assertNotEquals(first, admin.generateAccountNumber());
    }

    @Test
    void resetPinChangesPinAndUnlocks() {
        Account account = open("1234567890", AccountType.SAVINGS, "0");
        for (int i = 0; i < Account.MAX_FAILED_ATTEMPTS; i++) {
            account.registerFailedLogin();
        }
        assertTrue(account.isLocked());

        admin.resetPin("1234567890", "9876".toCharArray());

        assertFalse(account.isLocked());
        assertTrue(hasher.verify("9876".toCharArray(), account.pinHash()));
        assertFalse(hasher.verify("1234".toCharArray(), account.pinHash()));
    }

    @Test
    void resetPinForUnknownAccountFails() {
        assertThrows(AccountNotFoundException.class, () -> admin.resetPin("0000000000", "1111".toCharArray()));
    }

    @Test
    void interestIsPaidOnlyToSavingsWithPositiveBalance() {
        open("1000000001", AccountType.SAVINGS, "1000.00");   // 5% -> 50.00
        open("1000000002", AccountType.SAVINGS, "0");         // nothing to pay
        open("1000000003", AccountType.CHECKING, "1000.00");  // not a savings account

        List<Transaction> paid = admin.applyInterestToAllSavings();

        assertEquals(1, paid.size());
        assertEquals(new BigDecimal("50.00"), paid.get(0).amount());
        SavingsAccount s = (SavingsAccount) accounts.findByNumber("1000000001").orElseThrow();
        assertEquals(new BigDecimal("1050.00"), s.balance());
    }

    @Test
    void reportSummarisesActivity() {
        open("1000000001", AccountType.SAVINGS, "100.00");
        open("1000000002", AccountType.CHECKING, "50.00");
        BankService bank = new BankService(accounts, transactions, TestFixtures.CLOCK);
        bank.deposit("1000000001", new BigDecimal("10.00"));
        bank.withdraw("1000000002", new BigDecimal("20.00"));
        bank.transfer("1000000001", "1000000002", new BigDecimal("5.00"));
        accounts.findByNumber("1000000002").orElseThrow().restoreSecurityState(3, true);

        BankReport r = admin.generateReport();

        assertEquals(2, r.totalAccounts());
        assertEquals(1, r.lockedAccounts());
        assertEquals(new BigDecimal("140.00"), r.totalHoldings());
        assertEquals(3, r.depositCount()); // two opening deposits + one deposit
        assertEquals(new BigDecimal("160.00"), r.depositTotal());
        assertEquals(1, r.withdrawalCount());
        assertEquals(1, r.transferCount());
        assertEquals(new BigDecimal("5.00"), r.transferTotal());
    }
}
