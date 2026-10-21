package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.security.PinHasher;
import com.zainabtaha.banking.service.AuthService;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Administrator sign-in. On the very first launch there is no admin password yet, so this
 * screen asks the user to create one (replacing the hard-coded PIN of the original version).
 */
final class AdminLoginPanel extends JPanel {

    private final MainFrame frame;
    private final AuthService auth;

    private final JLabel heading = Theme.title("Administrator login");
    private final JLabel hint = Theme.subtitle(" ");
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel confirmLabel = new JLabel("Confirm password");
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JButton submit = new JButton("Log in");
    private boolean setupMode;

    AdminLoginPanel(MainFrame frame, AuthService auth) {
        super(new GridBagLayout());
        this.frame = frame;
        this.auth = auth;

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(Theme.card());

        GridBagConstraints h = Ui.cell(0, 0);
        h.gridwidth = 2;
        h.anchor = GridBagConstraints.LINE_START;
        form.add(heading, h);
        GridBagConstraints s = Ui.cell(0, 1);
        s.gridwidth = 2;
        s.anchor = GridBagConstraints.LINE_START;
        form.add(hint, s);

        form.add(new JLabel("Password"), Ui.cell(0, 2));
        form.add(passwordField, Ui.cell(1, 2));
        form.add(confirmLabel, Ui.cell(0, 3));
        form.add(confirmField, Ui.cell(1, 3));

        JButton back = new JButton("Back");
        submit.putClientProperty("JButton.buttonType", "default");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(submit);
        GridBagConstraints b = Ui.cell(1, 4);
        b.anchor = GridBagConstraints.LINE_END;
        b.fill = GridBagConstraints.NONE;
        form.add(buttons, b);

        back.addActionListener(e -> frame.showWelcome());
        submit.addActionListener(e -> submit());
        passwordField.addActionListener(e -> {
            if (setupMode) {
                confirmField.requestFocusInWindow();
            } else {
                submit.doClick();
            }
        });
        confirmField.addActionListener(e -> submit.doClick());

        add(form);
    }

    void reset() {
        passwordField.setText("");
        confirmField.setText("");
        setupMode = !auth.isAdminConfigured();
        heading.setText(setupMode ? "Set up administrator" : "Administrator login");
        hint.setText(setupMode
                ? "First launch: choose a password (8+ characters, letters and digits)."
                : "Enter the administrator password.");
        confirmLabel.setVisible(setupMode);
        confirmField.setVisible(setupMode);
        submit.setText(setupMode ? "Create & log in" : "Log in");
    }

    void focusFirstField() {
        passwordField.requestFocusInWindow();
    }

    private void submit() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        passwordField.setText("");
        confirmField.setText("");

        if (setupMode && !Arrays.equals(password, confirm)) {
            PinHasher.wipe(password);
            PinHasher.wipe(confirm);
            Ui.showError(this, new IllegalArgumentException("The two passwords do not match."));
            return;
        }
        boolean creating = setupMode;
        Ui.<Boolean>runAsync(this, () -> {
            try {
                if (creating) {
                    auth.configureAdmin(password);
                } else {
                    auth.loginAdmin(password);
                }
                return true;
            } finally {
                PinHasher.wipe(password);
                PinHasher.wipe(confirm);
            }
        }, ok -> frame.showAdminHome());
    }
}
