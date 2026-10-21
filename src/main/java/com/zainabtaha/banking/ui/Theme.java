package com.zainabtaha.banking.ui;

import java.awt.Color;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Look and feel, colours, fonts and formatting shared by every screen. */
public final class Theme {

    public static final Color PRIMARY = new Color(0x1F4E79);
    public static final Color POSITIVE = new Color(0x1B7F3B);
    public static final Color NEGATIVE = new Color(0xB3261E);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color CARD_BACKGROUND = new Color(0xF4F7FB);

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    private Theme() {
    }

    /**
     * Uses FlatLaf (a modern flat look and feel) when it is on the classpath and falls back to the
     * platform's native look otherwise. FlatLaf is loaded by name so the app still runs without it.
     */
    public static void install() {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("Table.showHorizontalLines", true);
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ignored) {
                // keep the default Metal look and feel
            }
        }
    }

    public static String money(java.math.BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    public static String dateTime(java.time.Instant instant) {
        return DATE_TIME.format(instant);
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 22f));
        label.setForeground(PRIMARY);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        return label;
    }

    public static JLabel big(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 30f));
        return label;
    }

    public static Border padding(int size) {
        return BorderFactory.createEmptyBorder(size, size, size, size);
    }

    public static Border card() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDE3EA), 1, true),
                padding(16));
    }
}
