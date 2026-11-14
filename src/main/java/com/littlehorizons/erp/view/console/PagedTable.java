package com.littlehorizons.erp.view.console;

import java.util.List;

/**
 * Shows a long list page by page (a school has 500+ students; printing them all at once is unreadable).
 */
// SOLID: SRP – paging only; it relies on ConsolePrinter and ConsoleInput for the real work
public class PagedTable {

    private static final int PAGE_SIZE = 15;

    private final ConsolePrinter printer;
    private final ConsoleInput input;

    public PagedTable(ConsolePrinter printer, ConsoleInput input) {
        this.printer = printer;
        this.input = input;
    }

    public void show(List<String> headers, List<List<String>> rows) {
        if (rows.size() <= PAGE_SIZE) {
            printer.table(headers, rows);
            return;
        }
        int total = rows.size();
        for (int from = 0; from < total; from += PAGE_SIZE) {
            int to = Math.min(from + PAGE_SIZE, total);
            printer.println(TableRenderer.render(headers, rows.subList(from, to)));
            printer.println("  Registros " + (from + 1) + "-" + to + " de " + total + ".");
            if (to < total) {
                String answer = input.readLine("ENTER = página siguiente, 0 = dejar de listar");
                if (answer.equals("0")) {
                    return;
                }
            }
        }
    }
}
