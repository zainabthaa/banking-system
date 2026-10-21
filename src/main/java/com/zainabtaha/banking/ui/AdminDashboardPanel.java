package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.BankReport;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Back-office screen: account list, summary figures and admin actions. */
final class AdminDashboardPanel extends JPanel {

    private final AdminService admin;
    private final AccountsModel model = new AccountsModel();
    private final JTable table = new JTable(model);
    private final JLabel accountsStat = Theme.big("0");
    private final JLabel holdingsStat = Theme.big("$0.00");
    private final JLabel lockedStat = Theme.big("0");

    AdminDashboardPanel(MainFrame frame, AdminService admin) {
        super(new BorderLayout(0, 16));
        this.admin = admin;
        setBorder(Theme.padding(24));

        // ----- header ------------------------------------------------------------------
        JPanel header = new JPanel(new BorderLayout());
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(Theme.title("Administrator"));
        titles.add(Theme.subtitle("Manage customer accounts"));
        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> frame.showWelcome());
        JPanel logoutWrap = new JPanel(new GridBagLayout());
        logoutWrap.add(logout);
        header.add(titles, BorderLayout.WEST);
        header.add(logoutWrap, BorderLayout.EAST);

        // ----- stats -------------------------------------------------------------------
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.add(statCard("Accounts", accountsStat));
        stats.add(statCard("Total holdings", holdingsStat));
        stats.add(statCard("Locked accounts", lockedStat));

        JPanel top = new JPanel(new BorderLayout(0, 16));
        top.add(header, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        // ----- table -------------------------------------------------------------------
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xDDE3EA)));

        // ----- actions -----------------------------------------------------------------
        JButton open = new JButton("Open account…");
        JButton resetPin = new JButton("Reset PIN…");
        JButton unlock = new JButton("Unlock");
        JButton interest = new JButton("Apply interest");
        JButton report = new JButton("Report");
        open.putClientProperty("JButton.buttonType", "default");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JButton b : new JButton[] {open, resetPin, unlock, interest, report}) {
            actions.add(b);
        }

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(actions, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        open.addActionListener(e -> {
            if (OpenAccountDialog.show(this, admin)) {
                refresh();
            }
        });
        resetPin.addActionListener(e -> selected().ifPresent(this::resetPin));
        unlock.addActionListener(e -> selected().ifPresent(a ->
                Ui.runAsync(this, () -> {
                    admin.unlock(a.accountNumber());
                    return a;
                }, x -> {
                    refresh();
                    Ui.info(this, "Account " + a.accountNumber() + " unlocked.");
                })));
        interest.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Pay one period of interest to every savings account?", "Apply interest",
                    JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) {
                Ui.runAsync(this, admin::applyInterestToAllSavings, paid -> {
                    refresh();
                    Ui.info(this, "Interest paid to " + paid.size() + " account(s).");
                });
            }
        });
        report.addActionListener(e -> showReport(admin.generateReport()));

        refresh();
    }

    private Optional<Account> selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Ui.showError(this, new IllegalArgumentException("Select an account in the table first."));
            return Optional.empty();
        }
        return Optional.of(model.get(table.convertRowIndexToModel(viewRow)));
    }

    private void resetPin(Account account) {
        JPasswordField pin = new JPasswordField(8);
        JPanel form = new JPanel(new GridBagLayout());
        form.add(new JLabel("New 4-digit PIN for " + account.holderName()), Ui.cell(0, 0));
        form.add(pin, Ui.cell(1, 0));
        int ok = JOptionPane.showConfirmDialog(this, form, "Reset PIN", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        char[] value = pin.getPassword();
        if (ok != JOptionPane.OK_OPTION) {
            PinHasher.wipe(value);
            return;
        }
        Ui.runAsync(this, () -> {
            try {
                admin.resetPin(account.accountNumber(), value);
                return account;
            } finally {
                PinHasher.wipe(value);
            }
        }, a -> {
            refresh();
            Ui.info(this, "PIN reset. The account is unlocked.");
        });
    }

    private void showReport(BankReport r) {
        String text = """
                Accounts:            %d  (%d locked)
                Total holdings:      %s

                Deposits:            %d  totalling %s
                Withdrawals:         %d  totalling %s
                Transfers:           %d  totalling %s
                Interest paid:       %s
                """.formatted(
                r.totalAccounts(), r.lockedAccounts(), Theme.money(r.totalHoldings()),
                r.depositCount(), Theme.money(r.depositTotal()),
                r.withdrawalCount(), Theme.money(r.withdrawalTotal()),
                r.transferCount(), Theme.money(r.transferTotal()),
                Theme.money(r.interestPaid()));
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 13));
        area.setOpaque(false);
        JOptionPane.showMessageDialog(this, area, "Bank report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refresh() {
        List<Account> accounts = admin.listAccounts();
        model.setRows(accounts);
        BankReport r = admin.generateReport();
        accountsStat.setText(Integer.toString(r.totalAccounts()));
        holdingsStat.setText(Theme.money(r.totalHoldings()));
        lockedStat.setText(Integer.toString(r.lockedAccounts()));
        lockedStat.setForeground(r.lockedAccounts() > 0 ? Theme.NEGATIVE : Theme.PRIMARY);
    }

    private static JPanel statCard(String caption, JLabel value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(Theme.card());
        card.setBackground(Theme.CARD_BACKGROUND);
        JLabel label = Theme.subtitle(caption);
        value.setForeground(Theme.PRIMARY);
        value.setFont(value.getFont().deriveFont(24f));
        card.add(label);
        card.add(value);
        return card;
    }

    private static final class AccountsModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Account number", "Holder", "Type", "Balance", "Status"};
        private List<Account> rows = List.of();

        void setRows(List<Account> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        Account get(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Account a = rows.get(row);
            return switch (column) {
                case 0 -> a.accountNumber();
                case 1 -> a.holderName();
                case 2 -> a.type().label();
                case 3 -> Theme.money(a.balance());
                default -> a.isLocked() ? "Locked" : "Active";
            };
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            if (!selected) {
                c.setForeground("Locked".equals(value) ? Theme.NEGATIVE : Theme.POSITIVE);
            }
            return c;
        }
    }
}
