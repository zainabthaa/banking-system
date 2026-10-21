package com.zainabtaha.banking.service;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.AdminCredentialStore;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.Exceptions.AccountLockedException;
import com.zainabtaha.banking.service.Exceptions.AuthenticationException;
import java.util.Optional;

/**
 * Customer and administrator authentication.
 *
 * <p>Failed customer logins are counted <em>on the account</em> and persisted, so the
 * three-strikes lockout survives an application restart (the original version only
 * counted attempts inside a single dialog loop).
 */
public final class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid account number or PIN.";

    private final AccountRepository accounts;
    private final AdminCredentialStore adminStore;
    private final PinHasher hasher;

    public AuthService(AccountRepository accounts, AdminCredentialStore adminStore, PinHasher hasher) {
        this.accounts = accounts;
        this.adminStore = adminStore;
        this.hasher = hasher;
    }

    /**
     * Logs a customer in.
     *
     * @return the authenticated account
     * @throws AccountLockedException  if the account is locked
     * @throws AuthenticationException if the account number or PIN is wrong
     */
    public Account loginCustomer(String accountNumber, char[] pin) {
        synchronized (accounts) { // same lock the other services use when mutating accounts
            return doLoginCustomer(accountNumber, pin);
        }
    }

    private Account doLoginCustomer(String accountNumber, char[] pin) {
        Optional<Account> found = accounts.findByNumber(accountNumber == null ? "" : accountNumber.strip());
        if (found.isEmpty()) {
            // Same message as a wrong PIN, so the login screen cannot be used to discover account numbers.
            throw new AuthenticationException(INVALID_CREDENTIALS);
        }
        Account account = found.get();
        if (account.isLocked()) {
            throw new AccountLockedException(account.accountNumber());
        }
        if (!hasher.verify(pin, account.pinHash())) {
            account.registerFailedLogin();
            accounts.save(account);
            if (account.isLocked()) {
                throw new AccountLockedException(account.accountNumber());
            }
            throw new AuthenticationException(INVALID_CREDENTIALS + " " + account.remainingAttempts()
                    + " attempt(s) remaining before the account is locked.");
        }
        if (account.failedLoginAttempts() > 0) {
            account.registerSuccessfulLogin();
            accounts.save(account);
        }
        return account;
    }

    public boolean isAdminConfigured() {
        return adminStore.loadPasswordHash().isPresent();
    }

    /** First-run setup: choose the admin password. Refuses to overwrite an existing one. */
    public synchronized void configureAdmin(char[] password) {
        if (isAdminConfigured()) {
            throw new IllegalStateException("Admin password is already configured.");
        }
        Validation.requireStrongAdminPassword(password);
        adminStore.savePasswordHash(hasher.hash(password));
    }

    public void loginAdmin(char[] password) {
        String hash = adminStore.loadPasswordHash()
                .orElseThrow(() -> new AuthenticationException("Admin account has not been set up yet."));
        if (!hasher.verify(password, hash)) {
            throw new AuthenticationException("Incorrect admin password.");
        }
    }

    public void changeAdminPassword(char[] currentPassword, char[] newPassword) {
        loginAdmin(currentPassword);
        Validation.requireStrongAdminPassword(newPassword);
        adminStore.savePasswordHash(hasher.hash(newPassword));
    }
}
