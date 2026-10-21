package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.Account;
import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AuthService;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

final class CustomerLoginPanel extends JPanel {

    private final JTextField accountField = new JTextField(16);
    private final JPasswordField pinField = new JPasswordField(16);

    CustomerLoginPanel(MainFrame frame, AuthService auth) {
        super(new GridBagLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(Theme.card());

        GridBagConstraints head = Ui.cell(0, 0);
        head.gridwidth = 2;
        head.anchor = GridBagConstraints.LINE_START;
        form.add(Theme.title("Customer login"), head);

        form.add(new JLabel("Account number"), Ui.cell(0, 1));
        form.add(accountField, Ui.cell(1, 1));
        form.add(new JLabel("PIN"), Ui.cell(0, 2));
        form.add(pinField, Ui.cell(1, 2));

        accountField.putClientProperty("JTextField.placeholderText", "10 digits");
        pinField.putClientProperty("JTextField.placeholderText", "4 digits");

        JButton back = new JButton("Back");
        JButton login = new JButton("Log in");
        login.putClientProperty("JButton.buttonType", "default");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(login);
        GridBagConstraints b = Ui.cell(1, 3);
        b.anchor = GridBagConstraints.LINE_END;
        b.fill = GridBagConstraints.NONE;
        form.add(buttons, b);

        back.addActionListener(e -> frame.showWelcome());
        login.addActionListener(e -> {
            String number = accountField.getText();
            char[] pin = pinField.getPassword();
            pinField.setText("");
            Ui.<Account>runAsync(this, () -> {
                try {
                    return auth.loginCustomer(number, pin);
                } finally {
                    PinHasher.wipe(pin);
                }
            }, frame::showCustomerHome);
        });
        // Enter in either field submits the form
        accountField.addActionListener(e -> pinField.requestFocusInWindow());
        pinField.addActionListener(e -> login.doClick());

        add(form);
    }

    void reset() {
        accountField.setText("");
        pinField.setText("");
    }

    void focusFirstField() {
        accountField.requestFocusInWindow();
    }
}
