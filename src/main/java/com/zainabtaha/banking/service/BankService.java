package com.zainabtaha.banking.service;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.Money;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.TransactionRepository;
import com.zainabtaha.banking.service.Exceptions.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customer-facing operations: deposits, withdrawals, transfers and statements.
 *
 * <p>Every operation follows the same sequence: validate, change the in-memory account,
 * persist the account(s), then append the ledger entries. All mutations synchronise on the
 * account repository so two operations can never interleave on the same balance.
 *
 * <p>A {@link Clock} is injected rather than calling {@code Instant.now()} directly, so tests
 * can control time.
 */
public final class BankService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final Clock clock;

    public BankService(AccountRepository accounts, TransactionRepository transactions, Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Account getAccount(String accountNumber) {
        return accounts.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public Transaction deposit(String accountNumber, BigDecimal amount) {
        synchronized (accounts) {
            Account account = getAccount(accountNumber);
            account.deposit(amount);
            accounts.save(account);
            Transaction t = Transaction.record(account, TransactionType.DEPOSIT, amount, clock.instant(), "");
            transactions.append(t);
            return t;
        }
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount) {
        synchronized (accounts) {
            Account account = getAccount(accountNumber);
            account.withdraw(amount); // throws InsufficientFundsException before anything changes
            accounts.save(account);
            Transaction t = Transaction.record(account, TransactionType.WITHDRAWAL, amount, clock.instant(), "");
            transactions.append(t);
            return t;
        }
    }

    /**
     * Moves money between two accounts. Both balances are written in a single file write,
     * so the transfer is never half-saved.
     *
     * @return the two ledger entries: [debit on source, credit on destination]
     */
    public List<Transaction> transfer(String fromNumber, String toNumber, BigDecimal amount) {
        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account.");
        }
        synchronized (accounts) {
            Account from = getAccount(fromNumber);
            Account to = getAccount(toNumber);
            BigDecimal value = Money.requirePositive(amount);

            from.withdraw(value);
            to.deposit(value);
            accounts.saveAll(List.of(from, to));

            var now = clock.instant();
            Transaction out = Transaction.record(from, TransactionType.TRANSFER_OUT, value, now,
                    "To " + to.accountNumber());
            Transaction in = Transaction.record(to, TransactionType.TRANSFER_IN, value, now,
                    "From " + from.accountNumber());
            transactions.appendAll(List.of(out, in));
            return List.of(out, in);
        }
    }

    /** Statement for one account, newest first. */
    public List<Transaction> history(String accountNumber) {
        List<Transaction> list = new ArrayList<>(transactions.findByAccount(accountNumber));
        Collections.reverse(list);
        return list;
    }
}
