package com.zainabtaha.banking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zainabtaha.banking.TestFixtures;
import com.zainabtaha.banking.TestFixtures.InMemoryAccounts;
import com.zainabtaha.banking.TestFixtures.InMemoryAdminStore;
import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.Exceptions.AccountLockedException;
import com.zainabtaha.banking.service.Exceptions.AuthenticationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final String NUMBER = "1234567890";

    private InMemoryAccounts accounts;
    private InMemoryAdminStore adminStore;
    private AuthService auth;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        adminStore = new InMemoryAdminStore();
        PinHasher hasher = TestFixtures.fastHasher();
        auth = new AuthService(accounts, adminStore, hasher);
        admin = new AdminService(accounts, new TestFixtures.InMemoryTransactions(), hasher, TestFixtures.CLOCK);
        admin.openAccount(new OpenAccountRequest(NUMBER, "Zainab", "1234".toCharArray(),
                AccountType.SAVINGS, BigDecimal.TEN, BigDecimal.ONE));
    }

    @Test
    void loginWithCorrectPin() {
        Account account = auth.loginCustomer(NUMBER, "1234".toCharArray());
        assertEquals(NUMBER, account.accountNumber());
    }

    @Test
    void unknownAccountGivesSameMessageAsWrongPin() {
        AuthenticationException unknown = assertThrows(AuthenticationException.class,
                () -> auth.loginCustomer("9999999999", "1234".toCharArray()));
        assertTrue(unknown.getMessage().startsWith("Invalid account number or PIN."));
    }

    @Test
    void wrongPinReportsRemainingAttempts() {
        AuthenticationException e = assertThrows(AuthenticationException.class,
                () -> auth.loginCustomer(NUMBER, "0000".toCharArray()));
        assertTrue(e.getMessage().contains("2 attempt(s) remaining"));
    }

    @Test
    void thirdWrongPinLocksTheAccountAndPersistsIt() {
        assertThrows(AuthenticationException.class, () -> auth.loginCustomer(NUMBER, "0000".toCharArray()));
        assertThrows(AuthenticationException.class, () -> auth.loginCustomer(NUMBER, "0000".toCharArray()));
        assertThrows(AccountLockedException.class, () -> auth.loginCustomer(NUMBER, "0000".toCharArray()));

        assertTrue(accounts.findByNumber(NUMBER).orElseThrow().isLocked());
        // Even the correct PIN is refused while locked
        assertThrows(AccountLockedException.class, () -> auth.loginCustomer(NUMBER, "1234".toCharArray()));
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        assertThrows(AuthenticationException.class, () -> auth.loginCustomer(NUMBER, "0000".toCharArray()));
        auth.loginCustomer(NUMBER, "1234".toCharArray());
        assertEquals(0, accounts.findByNumber(NUMBER).orElseThrow().failedLoginAttempts());
    }

    @Test
    void adminUnlockRestoresAccess() {
        for (int i = 0; i < 3; i++) {
            try {
                auth.loginCustomer(NUMBER, "0000".toCharArray());
            } catch (RuntimeException expected) {
                // keep failing until locked
            }
        }
        admin.unlock(NUMBER);
        assertEquals(NUMBER, auth.loginCustomer(NUMBER, "1234".toCharArray()).accountNumber());
    }

    @Test
    void adminSetupAndLogin() {
        assertFalse(auth.isAdminConfigured());
        assertThrows(AuthenticationException.class, () -> auth.loginAdmin("anything1".toCharArray()));

        auth.configureAdmin("secret123".toCharArray());
        assertTrue(auth.isAdminConfigured());
        auth.loginAdmin("secret123".toCharArray());
        assertThrows(AuthenticationException.class, () -> auth.loginAdmin("wrong1234".toCharArray()));
        assertFalse(adminStore.loadPasswordHash().orElseThrow().contains("secret123"));
    }

    @Test
    void adminSetupCannotBeRepeated() {
        auth.configureAdmin("secret123".toCharArray());
        assertThrows(IllegalStateException.class, () -> auth.configureAdmin("other1234".toCharArray()));
    }

    @Test
    void weakAdminPasswordsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> auth.configureAdmin("short1".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.configureAdmin("lettersonly".toCharArray()));
        assertThrows(IllegalArgumentException.class, () -> auth.configureAdmin("12345678".toCharArray()));
    }

    @Test
    void adminCanChangePassword() {
        auth.configureAdmin("secret123".toCharArray());
        auth.changeAdminPassword("secret123".toCharArray(), "newpass456".toCharArray());
        auth.loginAdmin("newpass456".toCharArray());
        assertThrows(AuthenticationException.class, () -> auth.loginAdmin("secret123".toCharArray()));
    }
}
