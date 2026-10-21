package com.zainabtaha.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zainabtaha.banking.TestFixtures;
import com.zainabtaha.banking.TestFixtures.InMemoryAccounts;
import com.zainabtaha.banking.TestFixtures.InMemoryTransactions;
import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.model.InsufficientFundsException;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.service.Exceptions.AccountNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankServiceTest {

    private static final String SAVINGS = "1000000001";
    private static final String CHECKING = "1000000002";

    private InMemoryAccounts accounts;
    private InMemoryTransactions transactions;
    private BankService bank;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        transactions = new InMemoryTransactions();
        bank = new BankService(accounts, transactions, TestFixtures.CLOCK);
        AdminService admin = new AdminService(accounts, transactions, TestFixtures.fastHasher(), TestFixtures.CLOCK);
        admin.openAccount(new OpenAccountRequest(SAVINGS, "Sara", "1111".toCharArray(),
                AccountType.SAVINGS, new BigDecimal("100.00"), new BigDecimal("2")));
        admin.openAccount(new OpenAccountRequest(CHECKING, "Omar", "2222".toCharArray(),
                AccountType.CHECKING, new BigDecimal("50.00"), new BigDecimal("200")));
    }

    @Test
    void depositIsSavedAndLogged() {
        Transaction t = bank.deposit(SAVINGS, new BigDecimal("25.50"));

        assertEquals(new BigDecimal("125.50"), bank.getAccount(SAVINGS).balance());
        assertEquals(TransactionType.DEPOSIT, t.type());
        assertEquals(new BigDecimal("125.50"), t.balanceAfter());
        assertEquals(TestFixtures.NOW, t.timestamp());
    }

    @Test
    void withdrawalBeyondFundsChangesNothing() {
        int before = transactions.findAll().size();
        assertThrows(InsufficientFundsException.class, () -> bank.withdraw(SAVINGS, new BigDecimal("100.01")));
        assertEquals(new BigDecimal("100.00"), bank.getAccount(SAVINGS).balance());
        assertEquals(before, transactions.findAll().size());
    }

    @Test
    void transferMovesMoneyAndLogsBothSides() {
        List<Transaction> legs = bank.transfer(SAVINGS, CHECKING, new BigDecimal("40.00"));

        assertEquals(new BigDecimal("60.00"), bank.getAccount(SAVINGS).balance());
        assertEquals(new BigDecimal("90.00"), bank.getAccount(CHECKING).balance());
        assertEquals(TransactionType.TRANSFER_OUT, legs.get(0).type());
        assertEquals(TransactionType.TRANSFER_IN, legs.get(1).type());
        assertEquals("To " + CHECKING, legs.get(0).description());
    }

    @Test
    void transferCanUseCheckingOverdraft() {
        bank.transfer(CHECKING, SAVINGS, new BigDecimal("230.00"));
        assertEquals(new BigDecimal("-180.00"), bank.getAccount(CHECKING).balance());
        assertEquals(new BigDecimal("330.00"), bank.getAccount(SAVINGS).balance());
    }

    @Test
    void failedTransferLeavesBothAccountsUntouched() {
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer(SAVINGS, CHECKING, new BigDecimal("500.00")));
        assertEquals(new BigDecimal("100.00"), bank.getAccount(SAVINGS).balance());
        assertEquals(new BigDecimal("50.00"), bank.getAccount(CHECKING).balance());
    }

    @Test
    void transferToUnknownAccountIsRejectedBeforeAnyChange() {
        assertThrows(AccountNotFoundException.class,
                () -> bank.transfer(SAVINGS, "9999999999", new BigDecimal("10.00")));
        assertEquals(new BigDecimal("100.00"), bank.getAccount(SAVINGS).balance());
    }

    @Test
    void cannotTransferToSelf() {
        assertThrows(IllegalArgumentException.class, () -> bank.transfer(SAVINGS, SAVINGS, BigDecimal.ONE));
    }

    @Test
    void historyIsNewestFirstAndOnlyForThatAccount() {
        bank.deposit(SAVINGS, new BigDecimal("1"));
        bank.withdraw(SAVINGS, new BigDecimal("2"));
        bank.deposit(CHECKING, new BigDecimal("3"));

        List<Transaction> history = bank.history(SAVINGS);
        assertEquals(3, history.size()); // opening deposit + 2
        assertEquals(TransactionType.WITHDRAWAL, history.get(0).type());
        assertEquals(TransactionType.OPENING_DEPOSIT, history.get(2).type());
    }
}
