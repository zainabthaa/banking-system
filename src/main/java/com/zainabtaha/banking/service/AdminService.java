package com.zainabtaha.banking.service;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.CheckingAccount;
import com.zainabtaha.banking.model.Money;
import com.zainabtaha.banking.model.SavingsAccount;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.TransactionRepository;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.Exceptions.AccountNotFoundException;
import com.zainabtaha.banking.service.Exceptions.DuplicateAccountException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

/** Back-office operations available to an authenticated administrator. */
public final class AdminService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final PinHasher hasher;
    private final Clock clock;
    private final RandomGenerator random;

    public AdminService(AccountRepository accounts, TransactionRepository transactions,
                        PinHasher hasher, Clock clock) {
        this(accounts, transactions, hasher, clock, new SecureRandom());
    }

    AdminService(AccountRepository accounts, TransactionRepository transactions,
                 PinHasher hasher, Clock clock, RandomGenerator random) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.hasher = hasher;
        this.clock = clock;
        this.random = random;
    }

    public Account openAccount(OpenAccountRequest request) {
        Validation.requireValidPin(request.pin());
        if (request.type() == null) {
            throw new IllegalArgumentException("Choose an account type.");
        }
        BigDecimal initial = request.initialDeposit() == null ? Money.zero() : request.initialDeposit();
        if (initial.signum() < 0) {
            throw new IllegalArgumentException("Initial deposit cannot be negative.");
        }
        BigDecimal option = request.rateOrLimit() == null ? BigDecimal.ZERO : request.rateOrLimit();
        String number = request.accountNumber() == null ? "" : request.accountNumber().strip();

        synchronized (accounts) {
            if (accounts.exists(number)) {
                throw new DuplicateAccountException(number);
            }
            String pinHash = hasher.hash(request.pin());
            Account account = switch (request.type()) {
                case SAVINGS -> new SavingsAccount(number, request.holderName(), Money.zero(), pinHash, option);
                case CHECKING -> new CheckingAccount(number, request.holderName(), Money.zero(), pinHash, option);
            };
            if (initial.signum() > 0) {
                account.deposit(initial);
            }
            accounts.save(account);
            if (initial.signum() > 0) {
                transactions.append(Transaction.record(account, TransactionType.OPENING_DEPOSIT, initial,
                        clock.instant(), "Account opened"));
            }
            return account;
        }
    }

    /** Suggests an unused 10-digit account number (first digit never 0). */
    public String generateAccountNumber() {
        synchronized (accounts) {
            for (int attempt = 0; attempt < 1_000; attempt++) {
                long n = 1_000_000_000L + random.nextLong(9_000_000_000L);
                String candidate = Long.toString(n);
                if (!accounts.exists(candidate)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Could not find a free account number.");
    }

    /** Sets a new PIN. Also unlocks the account, since a PIN reset is the usual fix for a lockout. */
    public void resetPin(String accountNumber, char[] newPin) {
        Validation.requireValidPin(newPin);
        synchronized (accounts) {
            Account account = find(accountNumber);
            account.changePinHash(hasher.hash(newPin));
            account.unlock();
            accounts.save(account);
        }
    }

    public void unlock(String accountNumber) {
        synchronized (accounts) {
            Account account = find(accountNumber);
            account.unlock();
            accounts.save(account);
        }
    }

    public List<Account> listAccounts() {
        return accounts.findAll().stream()
                .sorted(Comparator.comparing(Account::holderName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Pays one period of interest to every savings account with a positive balance.
     *
     * @return the interest transactions created
     */
    public List<Transaction> applyInterestToAllSavings() {
        synchronized (accounts) {
            Instant now = clock.instant();
            List<Account> changed = new ArrayList<>();
            List<Transaction> ledger = new ArrayList<>();
            for (Account account : accounts.findAll()) {
                if (account instanceof SavingsAccount savings) {
                    BigDecimal interest = savings.calculateInterest();
                    if (interest.signum() > 0) {
                        savings.deposit(interest);
                        changed.add(savings);
                        ledger.add(Transaction.record(savings, TransactionType.INTEREST, interest, now,
                                savings.interestRatePercent().stripTrailingZeros().toPlainString() + "% interest"));
                    }
                }
            }
            if (!changed.isEmpty()) {
                accounts.saveAll(changed);
                transactions.appendAll(ledger);
            }
            return ledger;
        }
    }

    public BankReport generateReport() {
        List<Account> all = accounts.findAll();
        BigDecimal holdings = all.stream().map(Account::balance).reduce(Money.zero(), BigDecimal::add);
        int locked = (int) all.stream().filter(Account::isLocked).count();

        int deposits = 0;
        int withdrawals = 0;
        int transfers = 0;
        BigDecimal depositTotal = Money.zero();
        BigDecimal withdrawalTotal = Money.zero();
        BigDecimal transferTotal = Money.zero();
        BigDecimal interest = Money.zero();
        for (Transaction t : transactions.findAll()) {
            switch (t.type()) {
                case DEPOSIT, OPENING_DEPOSIT -> {
                    deposits++;
                    depositTotal = depositTotal.add(t.amount());
                }
                case WITHDRAWAL -> {
                    withdrawals++;
                    withdrawalTotal = withdrawalTotal.add(t.amount());
                }
                case TRANSFER_OUT -> { // count each transfer once, on its debit side
                    transfers++;
                    transferTotal = transferTotal.add(t.amount());
                }
                case INTEREST -> interest = interest.add(t.amount());
                case TRANSFER_IN -> {
                    // already counted via TRANSFER_OUT
                }
            }
        }
        return new BankReport(all.size(), locked, holdings, deposits, depositTotal,
                withdrawals, withdrawalTotal, transfers, transferTotal, interest);
    }

    private Account find(String accountNumber) {
        return accounts.findByNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
}
