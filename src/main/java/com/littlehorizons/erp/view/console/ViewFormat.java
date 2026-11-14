package com.littlehorizons.erp.view.console;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Small helpers that turn values into the text shown on screen (dates, Colombian pesos, yes/no, bars).
 */
// SOLID: SRP – presentation formats only
public final class ViewFormat {

    private static final DecimalFormatSymbols CO_SYMBOLS = new DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));

    private ViewFormat() {
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(ConsoleInput.DATE_FORMAT);
    }

    /** 1250000 → $1.250.000 (a new formatter each time because DecimalFormat is not thread-safe). */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return new DecimalFormat("$#,##0.##", CO_SYMBOLS).format(amount);
    }

    public static String score(BigDecimal score) {
        return score == null ? "-" : score.toPlainString();
    }

    public static String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }

    public static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    /** Text bar for reports: fraction 0.0-1.0 → ████░░░░░░ */
    public static String bar(double fraction, int width) {
        int filled = (int) Math.round(Math.max(0, Math.min(1, fraction)) * width);
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    public static String percent(double fraction) {
        return String.format(Locale.US, "%.1f%%", fraction * 100);
    }
}
