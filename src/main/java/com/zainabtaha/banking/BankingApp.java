package com.zainabtaha.banking;

import com.zainabtaha.banking.repository.AccountRepository;
import com.zainabtaha.banking.repository.TransactionRepository;
import com.zainabtaha.banking.repository.file.CsvAccountRepository;
import com.zainabtaha.banking.repository.file.CsvTransactionRepository;
import com.zainabtaha.banking.repository.file.FileAdminCredentialStore;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.AuthService;
import com.zainabtaha.banking.service.BankService;
import com.zainabtaha.banking.ui.MainFrame;
import com.zainabtaha.banking.ui.Theme;
import java.nio.file.Path;
import java.time.Clock;
import javax.swing.SwingUtilities;

/**
 * Application entry point and composition root: this is the only place that knows which
 * concrete classes are used. Everything else receives its dependencies through constructors.
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code banking.dataDir} - where CSV files are stored (default {@code ./data})</li>
 *   <li>{@code banking.demo} - set to {@code false} to skip creating demo accounts</li>
 * </ul>
 */
public final class BankingApp {

    private BankingApp() {
    }

    public static void main(String[] args) {
        Path dataDir = Path.of(System.getProperty("banking.dataDir", "data"));

        AccountRepository accounts = new CsvAccountRepository(dataDir.resolve("accounts.csv"));
        TransactionRepository transactions = new CsvTransactionRepository(dataDir.resolve("transactions.csv"));
        FileAdminCredentialStore adminStore = new FileAdminCredentialStore(dataDir.resolve("admin.hash"));
        PinHasher hasher = new PinHasher();
        Clock clock = Clock.systemUTC();

        AuthService auth = new AuthService(accounts, adminStore, hasher);
        BankService bank = new BankService(accounts, transactions, clock);
        AdminService admin = new AdminService(accounts, transactions, hasher, clock);

        if (!"false".equalsIgnoreCase(System.getProperty("banking.demo"))) {
            DemoData.seedIfEmpty(accounts, admin);
        }

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame(auth, bank, admin).setVisible(true);
        });
    }
}
