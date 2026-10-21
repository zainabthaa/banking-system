package com.zainabtaha.banking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.repository.file.CsvAccountRepository;
import com.zainabtaha.banking.repository.file.CsvTransactionRepository;
import com.zainabtaha.banking.repository.file.FileAdminCredentialStore;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.AuthService;
import com.zainabtaha.banking.service.BankService;
import com.zainabtaha.banking.service.Exceptions.AccountLockedException;
import com.zainabtaha.banking.service.Exceptions.AuthenticationException;
import com.zainabtaha.banking.service.OpenAccountRequest;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Wires the real file-backed repositories together and simulates restarting the app. */
class EndToEndTest {

    @TempDir
    Path dataDir;

    private final PinHasher hasher = TestFixtures.fastHasher();

    private record App(AuthService auth, BankService bank, AdminService admin) {
    }

    private App start() {
        var accounts = new CsvAccountRepository(dataDir.resolve("accounts.csv"));
        var transactions = new CsvTransactionRepository(dataDir.resolve("transactions.csv"));
        var adminStore = new FileAdminCredentialStore(dataDir.resolve("admin.hash"));
        return new App(
                new AuthService(accounts, adminStore, hasher),
                new BankService(accounts, transactions, TestFixtures.CLOCK),
                new AdminService(accounts, transactions, hasher, TestFixtures.CLOCK));
    }

    @Test
    void balancesAndHistorySurviveRestart() {
        App first = start();
        first.admin().openAccount(new OpenAccountRequest("1000000001", "Sara", "1234".toCharArray(),
                AccountType.SAVINGS, new BigDecimal("100"), new BigDecimal("2")));
        first.admin().openAccount(new OpenAccountRequest("1000000002", "Omar", "4321".toCharArray(),
                AccountType.CHECKING, BigDecimal.ZERO, new BigDecimal("50")));
        first.bank().transfer("1000000001", "1000000002", new BigDecimal("30"));

        App second = start();
        second.auth().loginCustomer("1000000002", "4321".toCharArray());
        assertEquals(new BigDecimal("70.00"), second.bank().getAccount("1000000001").balance());
        assertEquals(new BigDecimal("30.00"), second.bank().getAccount("1000000002").balance());
        assertEquals(2, second.bank().history("1000000001").size());
    }

    @Test
    void lockoutSurvivesRestart() {
        App first = start();
        first.admin().openAccount(new OpenAccountRequest("1000000001", "Sara", "1234".toCharArray(),
                AccountType.SAVINGS, BigDecimal.ZERO, BigDecimal.ONE));
        assertThrows(AuthenticationException.class, () -> first.auth().loginCustomer("1000000001", "0000".toCharArray()));
        assertThrows(AuthenticationException.class, () -> first.auth().loginCustomer("1000000001", "0000".toCharArray()));

        // Restarting the app must not reset the counter
        App second = start();
        assertThrows(AccountLockedException.class, () -> second.auth().loginCustomer("1000000001", "0000".toCharArray()));

        App third = start();
        assertThrows(AccountLockedException.class, () -> third.auth().loginCustomer("1000000001", "1234".toCharArray()));
        third.admin().unlock("1000000001");
        third.auth().loginCustomer("1000000001", "1234".toCharArray());
    }
}
