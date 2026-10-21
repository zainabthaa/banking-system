package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.model.AccountType;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AdminService;
import com.zainabtaha.banking.service.OpenAccountRequest;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Modal form for opening a new customer account. */
final class OpenAccountDialog extends JDialog {

    private final AdminService admin;
    private final JTextField numberField = new JTextField(14);
    private final JTextField nameField = new JTextField(14);
    private final JComboBox<AccountType> typeBox = new JComboBox<>(AccountType.values());
    private final JPasswordField pinField = new JPasswordField(14);
    private final JPasswordField confirmPinField = new JPasswordField(14);
    private final JTextField depositField = new JTextField("0.00", 14);
    private final JLabel optionLabel = new JLabel();
    private final JTextField optionField = new JTextField(14);
    private final JPanel content = new JPanel(new BorderLayout(0, 12));
    private boolean created;

    /** @return true if an account was created */
    static boolean show(Component parent, AdminService admin) {
        OpenAccountDialog dialog = new OpenAccountDialog(parent, admin);
        dialog.setVisible(true); // blocks until closed
        return dialog.created;
    }

    private OpenAccountDialog(Component parent, AdminService admin) {
        super(SwingUtilities.getWindowAncestor(parent), "Open account", ModalityType.APPLICATION_MODAL);
        this.admin = admin;

        JButton generate = new JButton("Generate");
        generate.addActionListener(e -> numberField.setText(admin.generateAccountNumber()));
        JPanel numberRow = new JPanel(new BorderLayout(6, 0));
        numberRow.add(numberField, BorderLayout.CENTER);
        numberRow.add(generate, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        form.add(new JLabel("Account number"), Ui.cell(0, row));
        form.add(numberRow, Ui.cell(1, row++));
        form.add(new JLabel("Holder name"), Ui.cell(0, row));
        form.add(nameField, Ui.cell(1, row++));
        form.add(new JLabel("Account type"), Ui.cell(0, row));
        form.add(typeBox, Ui.cell(1, row++));
        form.add(optionLabel, Ui.cell(0, row));
        form.add(optionField, Ui.cell(1, row++));
        form.add(new JLabel("Initial deposit"), Ui.cell(0, row));
        form.add(depositField, Ui.cell(1, row++));
        form.add(new JLabel("PIN"), Ui.cell(0, row));
        form.add(pinField, Ui.cell(1, row++));
        form.add(new JLabel("Confirm PIN"), Ui.cell(0, row));
        form.add(confirmPinField, Ui.cell(1, row));

        typeBox.addActionListener(e -> updateOptionField());
        updateOptionField();
        numberField.setText(admin.generateAccountNumber());

        JButton cancel = new JButton("Cancel");
        JButton create = new JButton("Create account");
        create.putClientProperty("JButton.buttonType", "default");
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> submit());
        getRootPane().setDefaultButton(create);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(create);

        content.setBorder(Theme.padding(16));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void updateOptionField() {
        if (typeBox.getSelectedItem() == AccountType.SAVINGS) {
            optionLabel.setText("Interest rate (%)");
            optionField.setText("2.5");
        } else {
            optionLabel.setText("Overdraft limit");
            optionField.setText("500.00");
        }
    }

    private void submit() {
        char[] pin = pinField.getPassword();
        char[] confirm = confirmPinField.getPassword();
        try {
            if (!Arrays.equals(pin, confirm)) {
                throw new IllegalArgumentException("The two PINs do not match.");
            }
            if (!Account.isValidAccountNumber(numberField.getText().strip())) {
                throw new IllegalArgumentException("Account number must be exactly 10 digits.");
            }
            BigDecimal deposit = depositField.getText().isBlank() ? BigDecimal.ZERO : Ui.parseAmount(depositField.getText());
            BigDecimal option = Ui.parseAmount(optionField.getText());
            OpenAccountRequest request = new OpenAccountRequest(numberField.getText(), nameField.getText(),
                    pin, (AccountType) typeBox.getSelectedItem(), deposit, option);

            Ui.runAsync(content, () -> {
                try {
                    return admin.openAccount(request);
                } finally {
                    PinHasher.wipe(pin);
                    PinHasher.wipe(confirm);
                }
            }, account -> {
                created = true;
                dispose();
                Ui.info(getOwner(), "Opened " + account.type().label().toLowerCase()
                        + " account " + account.accountNumber() + " for " + account.holderName() + ".");
            });
        } catch (IllegalArgumentException ex) {
            PinHasher.wipe(pin);
            PinHasher.wipe(confirm);
            Ui.showError(this, ex);
        }
    }
}
