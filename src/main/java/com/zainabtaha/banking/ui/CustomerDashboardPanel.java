package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.CheckingAccount;
import com.zainabtaha.banking.model.SavingsAccount;
import com.zainabtaha.banking.model.Transaction;
import com.zainabtaha.banking.service.BankService;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Home screen for a logged-in customer: balance, quick actions and statement. */
final class CustomerDashboardPanel extends JPanel {

    private final BankService bank;
    private final String accountNumber;

    private final JLabel balanceLabel = Theme.big("");
    private final JLabel detailLabel = Theme.subtitle("");
    private final JTextField amountField = new JTextField(10);
    private final StatementModel statement = new StatementModel();

    CustomerDashboardPanel(MainFrame frame, BankService bank, String accountNumber) {
        super(new BorderLayout(0, 16));
        this.bank = bank;
        this.accountNumber = accountNumber;
        setBorder(Theme.padding(24));

        Account account = bank.getAccount(accountNumber);

        // ----- header ------------------------------------------------------------------
        JPanel header = new JPanel(new BorderLayout());
        JPanel who = new JPanel();
        who.setLayout(new BoxLayout(who, BoxLayout.Y_AXIS));
        who.add(Theme.title("Welcome, " + account.holderName()));
        who.add(Theme.subtitle(account.type().label() + " account  ·  " + account.accountNumber()));
        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> frame.showWelcome());
        JPanel logoutWrap = new JPanel(new GridBagLayout());
        logoutWrap.add(logout);
        header.add(who, BorderLayout.WEST);
        header.add(logoutWrap, BorderLayout.EAST);

        // ----- balance card + actions ----------------------------------------------------
        JPanel balanceCard = new JPanel();
        balanceCard.setLayout(new BoxLayout(balanceCard, BoxLayout.Y_AXIS));
        balanceCard.setBorder(Theme.card());
        balanceCard.setBackground(Theme.CARD_BACKGROUND);
        JLabel caption = Theme.subtitle("Current balance");
        balanceCard.add(caption);
        balanceCard.add(balanceLabel);
        balanceCard.add(detailLabel);
        for (Component c : balanceCard.getComponents()) {
            ((JLabel) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        amountField.putClientProperty("JTextField.placeholderText", "0.00");
        JButton deposit = new JButton("Deposit");
        JButton withdraw = new JButton("Withdraw");
        JButton transfer = new JButton("Transfer…");
        deposit.putClientProperty("JButton.buttonType", "default");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(new JLabel("Amount"));
        actions.add(amountField);
        actions.add(deposit);
        actions.add(withdraw);
        actions.add(transfer);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.add(header, BorderLayout.NORTH);
        top.add(balanceCard, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        // ----- statement -----------------------------------------------------------------
        JTable table = new JTable(statement);
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(2).setCellRenderer(new AmountRenderer());
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(right);

        JPanel statementPanel = new JPanel(new BorderLayout(0, 8));
        JLabel statementTitle = new JLabel("Recent transactions");
        statementTitle.setFont(statementTitle.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        statementPanel.add(statementTitle, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xDDE3EA)));
        statementPanel.add(scroll, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(statementPanel, BorderLayout.CENTER);

        // ----- behaviour -----------------------------------------------------------------
        deposit.addActionListener(e -> withAmount(amount ->
                Ui.runAsync(this, () -> bank.deposit(accountNumber, amount), t -> done("Deposited " + Theme.money(amount)))));
        withdraw.addActionListener(e -> withAmount(amount ->
                Ui.runAsync(this, () -> bank.withdraw(accountNumber, amount), t -> done("Withdrew " + Theme.money(amount)))));
        transfer.addActionListener(e -> openTransferDialog());
        amountField.addActionListener(e -> deposit.doClick());

        refresh();
    }

    private void withAmount(java.util.function.Consumer<BigDecimal> action) {
        try {
            action.accept(Ui.parseAmount(amountField.getText()));
        } catch (IllegalArgumentException ex) {
            Ui.showError(this, ex);
        }
    }

    private void openTransferDialog() {
        JTextField to = new JTextField(14);
        JTextField amount = new JTextField(amountField.getText(), 14);
        JPanel form = new JPanel(new GridBagLayout());
        form.add(new JLabel("To account"), Ui.cell(0, 0));
        form.add(to, Ui.cell(1, 0));
        form.add(new JLabel("Amount"), Ui.cell(0, 1));
        form.add(amount, Ui.cell(1, 1));

        int choice = JOptionPane.showConfirmDialog(this, form, "Transfer money",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            BigDecimal value = Ui.parseAmount(amount.getText());
            String destination = to.getText().strip();
            Ui.runAsync(this, () -> bank.transfer(accountNumber, destination, value),
                    t -> done("Transferred " + Theme.money(value) + " to " + destination));
        } catch (IllegalArgumentException ex) {
            Ui.showError(this, ex);
        }
    }

    private void done(String message) {
        amountField.setText("");
        refresh();
        Ui.info(this, message);
    }

    private void refresh() {
        Account account = bank.getAccount(accountNumber);
        balanceLabel.setText(Theme.money(account.balance()));
        balanceLabel.setForeground(account.balance().signum() < 0 ? Theme.NEGATIVE : Theme.PRIMARY);
        if (account instanceof CheckingAccount checking) {
            detailLabel.setText("Overdraft limit " + Theme.money(checking.overdraftLimit())
                    + "  ·  Available " + Theme.money(checking.availableFunds()));
        } else if (account instanceof SavingsAccount savings) {
            detailLabel.setText("Interest rate " + savings.interestRatePercent().stripTrailingZeros().toPlainString() + "%");
        }
        statement.setRows(bank.history(accountNumber));
    }

    /** Table model backed directly by {@link Transaction} records. */
    private static final class StatementModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Date", "Type", "Amount", "Balance", "Details"};
        private List<Transaction> rows = List.of();

        void setRows(List<Transaction> rows) {
            this.rows = rows;
            fireTableDataChanged();
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
            Transaction t = rows.get(row);
            return switch (column) {
                case 0 -> Theme.dateTime(t.timestamp());
                case 1 -> t.type().label();
                case 2 -> t.signedAmount();
                case 3 -> Theme.money(t.balanceAfter());
                default -> t.description();
            };
        }
    }

    /** Shows credits in green with a + sign and debits in red. */
    private static final class AmountRenderer extends DefaultTableCellRenderer {
        AmountRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            BigDecimal amount = (BigDecimal) value;
            String text = (amount.signum() >= 0 ? "+" : "−") + Theme.money(amount.abs());
            Component c = super.getTableCellRendererComponent(table, text, selected, focus, row, column);
            if (!selected) {
                c.setForeground(amount.signum() >= 0 ? Theme.POSITIVE : Theme.NEGATIVE);
            }
            return c;
        }
    }
}
