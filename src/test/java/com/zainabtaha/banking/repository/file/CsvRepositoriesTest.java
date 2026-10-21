package com.zainabtaha.banking.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.CheckingAccount;
import com.zainabtaha.banking.model.SavingsAccount;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.model.TransactionType;
import com.zainabtaha.banking.repository.StorageException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvRepositoriesTest {

    @TempDir
    Path dir;

    @Test
    void accountsSurviveARestart() {
        Path file = dir.resolve("accounts.csv");
        CsvAccountRepository repo = new CsvAccountRepository(file);

        CheckingAccount checking = new CheckingAccount("1111111111", "Doe, Jane", new BigDecimal("-20.50"),
                "pbkdf2-sha256$1$c2FsdA==$aGFzaA==", new BigDecimal("100"));
        checking.registerFailedLogin();
        SavingsAccount savings = new SavingsAccount("2222222222", "Ali", new BigDecimal("10.00"),
                "pbkdf2-sha256$1$c2FsdA==$aGFzaA==", new BigDecimal("3.75"));
        repo.saveAll(List.of(checking, savings));

        CsvAccountRepository reloaded = new CsvAccountRepository(file);
        Account a = reloaded.findByNumber("1111111111").orElseThrow();
        assertTrue(a instanceof CheckingAccount);
        assertEquals("Doe, Jane", a.holderName());
        assertEquals(new BigDecimal("-20.50"), a.balance());
        assertEquals(new BigDecimal("100.00"), ((CheckingAccount) a).overdraftLimit());
        assertEquals(1, a.failedLoginAttempts());
        assertFalse(a.isLocked());

        SavingsAccount s = (SavingsAccount) reloaded.findByNumber("2222222222").orElseThrow();
        assertEquals(new BigDecimal("3.75"), s.interestRatePercent());
        assertEquals(2, reloaded.findAll().size());
    }

    @Test
    void saveReplacesExistingAccount() {
        Path file = dir.resolve("accounts.csv");
        CsvAccountRepository repo = new CsvAccountRepository(file);
        SavingsAccount s = new SavingsAccount("2222222222", "Ali", new BigDecimal("10.00"), "h", BigDecimal.ONE);
        repo.save(s);
        s.deposit(new BigDecimal("5"));
        repo.save(s);

        assertEquals(1, new CsvAccountRepository(file).findAll().size());
        assertEquals(new BigDecimal("15.00"), new CsvAccountRepository(file).findByNumber("2222222222").orElseThrow().balance());
    }

    @Test
    void missingFileMeansNoAccounts() {
        assertTrue(new CsvAccountRepository(dir.resolve("nope.csv")).findAll().isEmpty());
    }

    @Test
    void corruptFileFailsLoudly() throws IOException {
        Path file = dir.resolve("accounts.csv");
        Files.writeString(file, String.join(",", CsvAccountRepository.HEADER) + "\nonly,three,columns\n");
        assertThrows(StorageException.class, () -> new CsvAccountRepository(file));
    }

    @Test
    void transactionsAreAppendedAndReloaded() {
        Path file = dir.resolve("transactions.csv");
        CsvTransactionRepository repo = new CsvTransactionRepository(file);
        Instant t0 = Instant.parse("2025-01-01T00:00:00Z");
        repo.append(new Transaction("id-1", "1111111111", TransactionType.DEPOSIT,
                new BigDecimal("10"), new BigDecimal("10"), t0, ""));
        repo.appendAll(List.of(
                new Transaction("id-2", "1111111111", TransactionType.TRANSFER_OUT,
                        new BigDecimal("4"), new BigDecimal("6"), t0.plusSeconds(60), "To 2222222222"),
                new Transaction("id-3", "2222222222", TransactionType.TRANSFER_IN,
                        new BigDecimal("4"), new BigDecimal("4"), t0.plusSeconds(60), "From 1111111111")));

        CsvTransactionRepository reloaded = new CsvTransactionRepository(file);
        assertEquals(3, reloaded.findAll().size());
        List<Transaction> mine = reloaded.findByAccount("1111111111");
        assertEquals(2, mine.size());
        assertEquals("To 2222222222", mine.get(1).description());
        assertEquals(new BigDecimal("6.00"), mine.get(1).balanceAfter());
    }

    @Test
    void adminHashIsPersisted() {
        FileAdminCredentialStore store = new FileAdminCredentialStore(dir.resolve("admin.hash"));
        assertTrue(store.loadPasswordHash().isEmpty());
        store.savePasswordHash("pbkdf2-sha256$1$a$b");
        assertEquals("pbkdf2-sha256$1$a$b",
                new FileAdminCredentialStore(dir.resolve("admin.hash")).loadPasswordHash().orElseThrow());
    }
}
