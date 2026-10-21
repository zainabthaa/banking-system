package com.zainabtaha.banking.ui;

import com.zainabtaha.banking.model.BankingException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/** Small helpers that keep the screen classes short. */
final class Ui {

    private Ui() {
    }

    /**
     * Runs {@code work} on a background thread and hands the result back on the Swing event thread.
     * Used for anything slow (PIN hashing, file writes) so the window never freezes.
     */
    static <T> void runAsync(JComponent owner, Supplier<T> work, Consumer<T> onSuccess) {
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setEnabledDeep(owner, false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                owner.setCursor(Cursor.getDefaultCursor());
                setEnabledDeep(owner, true);
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    showError(owner, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Business errors get a friendly dialog; anything unexpected is reported as a bug. */
    static void showError(Component parent, Throwable error) {
        if (error instanceof BankingException || error instanceof IllegalArgumentException) {
            JOptionPane.showMessageDialog(parent, error.getMessage(), "Cannot complete request",
                    JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, "Unexpected error: " + error, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    static BigDecimal parseAmount(String text) {
        String cleaned = text == null ? "" : text.strip().replace(",", "").replace("$", "");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + text.strip() + "\" is not a valid amount.");
        }
    }

    static void setEnabledDeep(Component component, boolean enabled) {
        if (component instanceof JPanel panel) {
            for (Component child : panel.getComponents()) {
                setEnabledDeep(child, enabled);
            }
        } else if (!(component instanceof javax.swing.JLabel)) {
            component.setEnabled(enabled);
        }
    }

    /** GridBag constraints for a label/field form row. */
    static GridBagConstraints cell(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = x == 0 ? GridBagConstraints.LINE_END : GridBagConstraints.LINE_START;
        c.fill = x == 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        c.weightx = x == 0 ? 0 : 1;
        return c;
    }
}
