package com.zainabtaha.banking.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class WelcomePanel extends JPanel {

    WelcomePanel(MainFrame frame) {
        super(new GridBagLayout()); // centres the column below

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        JLabel title = Theme.title("Banking System");
        title.setFont(title.getFont().deriveFont(30f));
        JLabel subtitle = Theme.subtitle("Choose how you want to sign in");

        JButton customer = new JButton("Customer login");
        JButton admin = new JButton("Administrator login");
        customer.putClientProperty("JButton.buttonType", "default");
        customer.addActionListener(e -> frame.showCustomerLogin());
        admin.addActionListener(e -> frame.showAdminLogin());

        for (Component c : new Component[] {title, subtitle, customer, admin}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        Dimension buttonSize = new Dimension(260, 40);
        for (JButton b : new JButton[] {customer, admin}) {
            b.setMaximumSize(buttonSize);
            b.setPreferredSize(buttonSize);
        }

        column.add(title);
        column.add(Box.createVerticalStrut(6));
        column.add(subtitle);
        column.add(Box.createVerticalStrut(28));
        column.add(customer);
        column.add(Box.createVerticalStrut(12));
        column.add(admin);
        add(column);
    }
}
