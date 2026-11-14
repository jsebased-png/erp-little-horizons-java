package com.littlehorizons.erp.view.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns headers and rows of text into an aligned table drawn with box characters.
 * It exists so every list in the application looks the same and no view has to count spaces.
 */
// SOLID: SRP – only formats text; it never prints and never reads data
public final class TableRenderer {

    private static final int MAX_CELL_WIDTH = 32;

    private TableRenderer() {
    }

    /** Builds the table as one String (lines separated by \n). */
    public static String render(List<String> headers, List<List<String>> rows) {
        int columns = headers.size();
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = headers.get(c).length();
        }
        List<List<String>> cleanRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> clean = new ArrayList<>();
            for (int c = 0; c < columns; c++) {
                String cell = c < row.size() ? shorten(row.get(c)) : "-";
                clean.add(cell);
                widths[c] = Math.max(widths[c], cell.length());
            }
            cleanRows.add(clean);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(border('┌', '┬', '┐', widths)).append('\n');
        sb.append(line(headers, widths)).append('\n');
        sb.append(border('├', '┼', '┤', widths)).append('\n');
        for (List<String> row : cleanRows) {
            sb.append(line(row, widths)).append('\n');
        }
        sb.append(border('└', '┴', '┘', widths));
        return sb.toString();
    }

    private static String shorten(String text) {
        if (text == null || text.isBlank()) {
            return "-";
        }
        String oneLine = text.replace('\n', ' ').trim();
        return oneLine.length() <= MAX_CELL_WIDTH ? oneLine : oneLine.substring(0, MAX_CELL_WIDTH - 1) + "…";
    }

    private static String line(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder("│");
        for (int c = 0; c < widths.length; c++) {
            String cell = cells.get(c);
            sb.append(' ').append(cell).append(" ".repeat(widths[c] - cell.length())).append(" │");
        }
        return sb.toString();
    }

    private static String border(char left, char middle, char right, int[] widths) {
        StringBuilder sb = new StringBuilder().append(left);
        for (int c = 0; c < widths.length; c++) {
            sb.append("─".repeat(widths[c] + 2));
            sb.append(c < widths.length - 1 ? middle : right);
        }
        return sb.toString();
    }
}
