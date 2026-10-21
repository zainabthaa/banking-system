package com.zainabtaha.banking.repository;

import com.zainabtaha.banking.model.Account;
import java.util.List;
import java.util.Optional;

/**
 * Persistence boundary for accounts. Services depend on this interface, never on files,
 * so storage can be swapped (CSV today, a database tomorrow) and services can be unit-tested
 * against an in-memory implementation.
 */
public interface AccountRepository {

    Optional<Account> findByNumber(String accountNumber);

    List<Account> findAll();

    default boolean exists(String accountNumber) {
        return findByNumber(accountNumber).isPresent();
    }

    /** Inserts or replaces the account with the same number. */
    void save(Account account);

    /** Saves several accounts as one write, so a transfer never persists only one side. */
    void saveAll(List<? extends Account> accounts);
}
