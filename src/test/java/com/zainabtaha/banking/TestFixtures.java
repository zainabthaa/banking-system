package com.zainabtaha.banking;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.AdminCredentialStore;
import com.zainabtaha.banking.repository.TransactionRepository;
import com.zainabtaha.banking.security.PinHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory test doubles and shared fixtures. */
public final class TestFixtures {

    public static final Instant NOW = Instant.parse("2025-06-01T10:15:30Z");
    public static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private TestFixtures() {
    }

    /** A hasher with a tiny work factor so tests stay fast. Production uses 600,000 iterations. */
    public static PinHasher fastHasher() {
        return new PinHasher(1_000);
    }

    public static final class InMemoryAccounts implements AccountRepository {
        private final Map<String, Account> data = new LinkedHashMap<>();
        public int saveCalls;

        @Override
        public Optional<Account> findByNumber(String accountNumber) {
            return Optional.ofNullable(data.get(accountNumber));
        }

        @Override
        public List<Account> findAll() {
            return List.copyOf(data.values());
        }

        @Override
        public void save(Account account) {
            saveAll(List.of(account));
        }

        @Override
        public void saveAll(List<? extends Account> accounts) {
            saveCalls++;
            accounts.forEach(a -> data.put(a.accountNumber(), a));
        }
    }

    public static final class InMemoryTransactions implements TransactionRepository {
        private final List<Transaction> data = new ArrayList<>();

        @Override
        public void append(Transaction transaction) {
            data.add(transaction);
        }

        @Override
        public void appendAll(List<Transaction> transactions) {
            data.addAll(transactions);
        }

        @Override
        public List<Transaction> findByAccount(String accountNumber) {
            return data.stream().filter(t -> t.accountNumber().equals(accountNumber)).toList();
        }

        @Override
        public List<Transaction> findAll() {
            return List.copyOf(data);
        }
    }

    public static final class InMemoryAdminStore implements AdminCredentialStore {
        private String hash;

        @Override
        public Optional<String> loadPasswordHash() {
            return Optional.ofNullable(hash);
        }

        @Override
        public void savePasswordHash(String hash) {
            this.hash = hash;
        }
    }
}
