package com.zainabtaha.banking.repository.file;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.model.CheckingAccount;
import com.zainabtaha.banking.model.SavingsAccount;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.StorageException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores accounts in a single CSV file. The file is loaded once and kept in memory;
 * every change rewrites the file atomically.
 */
public final class CsvAccountRepository implements AccountRepository {

    static final List<String> HEADER = List.of(
            "account_number", "type", "holder_name", "balance", "pin_hash",
            "failed_attempts", "locked", "overdraft_limit", "interest_rate");

    private final Path file;
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public CsvAccountRepository(Path file) {
        this.file = file;
        for (List<String> row : Csv.readRows(file)) {
            Account account = fromRow(row);
            accounts.put(account.accountNumber(), account);
        }
    }

    @Override
    public synchronized Optional<Account> findByNumber(String accountNumber) {
        return Optional.ofNullable(accounts.get(accountNumber));
    }

    @Override
    public synchronized List<Account> findAll() {
        return List.copyOf(accounts.values());
    }

    @Override
    public synchronized void save(Account account) {
        saveAll(List.of(account));
    }

    @Override
    public synchronized void saveAll(List<? extends Account> toSave) {
        Map<String, Account> next = new LinkedHashMap<>(accounts);
        for (Account account : toSave) {
            next.put(account.accountNumber(), account);
        }
        List<List<String>> rows = new ArrayList<>();
        for (Account account : next.values()) {
            rows.add(toRow(account));
        }
        Csv.writeAtomically(file, HEADER, rows); // only update memory once the write succeeded
        accounts.clear();
        accounts.putAll(next);
    }

    private static List<String> toRow(Account a) {
        String overdraft = a instanceof CheckingAccount c ? c.overdraftLimit().toPlainString() : "";
        String interest = a instanceof SavingsAccount s ? s.interestRatePercent().toPlainString() : "";
        return List.of(
                a.accountNumber(),
                a.type().name(),
                a.holderName(),
                a.balance().toPlainString(),
                a.pinHash(),
                Integer.toString(a.failedLoginAttempts()),
                Boolean.toString(a.isLocked()),
                overdraft,
                interest);
    }

    private Account fromRow(List<String> r) {
        if (r.size() != HEADER.size()) {
            throw new StorageException("Corrupt row in " + file.getFileName() + ": expected "
                    + HEADER.size() + " columns but found " + r.size(), null);
        }
        try {
            String number = r.get(0);
            AccountType type = AccountType.valueOf(r.get(1));
            String name = r.get(2);
            BigDecimal balance = new BigDecimal(r.get(3));
            String pinHash = r.get(4);
            Account account = switch (type) {
                case SAVINGS -> new SavingsAccount(number, name, balance, pinHash, new BigDecimal(r.get(8)));
                case CHECKING -> new CheckingAccount(number, name, balance, pinHash, new BigDecimal(r.get(7)));
            };
            account.restoreSecurityState(Integer.parseInt(r.get(5)), Boolean.parseBoolean(r.get(6)));
            return account;
        } catch (IllegalArgumentException e) {
            throw new StorageException("Corrupt row in " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
