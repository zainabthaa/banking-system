package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.AuthService;
import com.zainabtaha.banking.service.BankService;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * The single application window. Screens are swapped with a {@link CardLayout} instead of
 * opening and disposing a new {@code JFrame} for every step, which is what the original did.
 */
public final class MainFrame extends JFrame {

    private static final String WELCOME = "welcome";
    private static final String CUSTOMER_LOGIN = "customer-login";
    private static final String ADMIN_LOGIN = "admin-login";
    private static final String CUSTOMER_HOME = "customer-home";
    private static final String ADMIN_HOME = "admin-home";

    private final AuthService auth;
    private final BankService bank;
    private final AdminService admin;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final CustomerLoginPanel customerLogin;
    private final AdminLoginPanel adminLogin;

    public MainFrame(AuthService auth, BankService bank, AdminService admin) {
        super("Banking System");
        this.auth = auth;
        this.bank = bank;
        this.admin = admin;

        customerLogin = new CustomerLoginPanel(this, auth);
        adminLogin = new AdminLoginPanel(this, auth);

        root.add(new WelcomePanel(this), WELCOME);
        root.add(customerLogin, CUSTOMER_LOGIN);
        root.add(adminLogin, ADMIN_LOGIN);
        setContentPane(root);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(820, 580));
        setSize(960, 640);
        setLocationRelativeTo(null);
        showWelcome();
    }

    void showWelcome() {
        cards.show(root, WELCOME);
    }

    void showCustomerLogin() {
        customerLogin.reset();
        cards.show(root, CUSTOMER_LOGIN);
        customerLogin.focusFirstField();
    }

    void showAdminLogin() {
        adminLogin.reset();
        cards.show(root, ADMIN_LOGIN);
        adminLogin.focusFirstField();
    }

    void showCustomerHome(Account account) {
        replaceCard(new CustomerDashboardPanel(this, bank, account.accountNumber()), CUSTOMER_HOME);
    }

    void showAdminHome() {
        replaceCard(new AdminDashboardPanel(this, admin), ADMIN_HOME);
    }

    /** Dashboards hold per-session state, so a fresh one is built on every login. */
    private void replaceCard(JPanel panel, String name) {
        for (var component : root.getComponents()) {
            if (name.equals(component.getName())) {
                root.remove(component);
            }
        }
        panel.setName(name);
        root.add(panel, name);
        cards.show(root, name);
    }
}
