package com.littlehorizons.erp.view.console;

import java.io.PrintStream;
import java.util.List;

/**
 * The ONLY class of the application that writes to the screen.
 * Views call it to show titles, menus, messages and tables; this keeps System.out out of every other class
 * and makes all the output look consistent.
 */
// SOLID: SRP – output only (input lives in ConsoleInput)
public class ConsolePrinter {

    private static final int BOX_WIDTH = 62;

    private final PrintStream out;

    public ConsolePrinter(PrintStream out) {
        this.out = out;
    }

    public void println(String text) {
        out.println(text);
    }

    public void print(String text) {
        out.print(text);
        out.flush();
    }

    public void blank() {
        out.println();
    }

    /** Big double-line box used for the application banner and for the title of each menu. */
    public void titleBox(String title) {
        out.println();
        out.println("╔" + "═".repeat(BOX_WIDTH) + "╗");
        out.println("║" + center(title, BOX_WIDTH) + "║");
        out.println("╚" + "═".repeat(BOX_WIDTH) + "╝");
    }

    /** Menu box: title, numbered options and the exit/back option (normally 0, or the last number in the main menu). */
    public void menuBox(String title, List<String> options, String exitLabel, int exitNumber) {
        out.println();
        out.println("╔" + "═".repeat(BOX_WIDTH) + "╗");
        out.println("║" + center(title, BOX_WIDTH) + "║");
        out.println("╠" + "═".repeat(BOX_WIDTH) + "╣");
        for (int i = 0; i < options.size(); i++) {
            out.println("║" + pad("  " + (i + 1) + ". " + options.get(i), BOX_WIDTH) + "║");
        }
        out.println("╟" + "─".repeat(BOX_WIDTH) + "╢");
        out.println("║" + pad("  " + exitNumber + ". " + exitLabel, BOX_WIDTH) + "║");
        out.println("╚" + "═".repeat(BOX_WIDTH) + "╝");
    }

    /** Small heading inside a screen. */
    public void section(String text) {
        out.println();
        out.println("── " + text + " " + "─".repeat(Math.max(3, BOX_WIDTH - text.length() - 4)));
    }

    public void success(String message) {
        out.println("  ✔ " + message);
    }

    public void error(String message) {
        out.println("  ✖ Error: " + message);
    }

    public void warning(String message) {
        out.println("  ⚠ " + message);
    }

    public void info(String message) {
        out.println("  ℹ " + message);
    }

    /** One "label: value" line of a detail screen. */
    public void field(String label, String value) {
        out.println(String.format("  %-22s %s", label + ":", value == null || value.isBlank() ? "-" : value));
    }

    /** Prints a table, or an info line when there is nothing to show. */
    public void table(List<String> headers, List<List<String>> rows) {
        if (rows.isEmpty()) {
            info("No hay registros para mostrar.");
            return;
        }
        out.println(TableRenderer.render(headers, rows));
        out.println("  " + rows.size() + " registro(s).");
    }

    private String center(String text, int width) {
        int total = Math.max(0, width - text.length());
        int left = total / 2;
        return " ".repeat(left) + text + " ".repeat(total - left);
    }

    private String pad(String text, int width) {
        return text.length() >= width ? text.substring(0, width) : text + " ".repeat(width - text.length());
    }
}
