package com.littlehorizons.erp.view.console;

import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * A numbered menu. Each option is a label plus the action (a lambda) to run when it is chosen.
 * The loop also protects the whole application: any error thrown by an action is shown as a message and logged,
 * and the menu is drawn again, so a failed operation never closes the program.
 */
// Pattern: Command – each option stores an action (Runnable) that the menu runs without knowing what it does
// SOLID: OCP – adding an option means adding one line with add(...), the loop does not change
public class Menu {

    private record Option(String label, Runnable action, boolean pauseAfter) {
    }

    private final String title;
    private final String exitLabel;
    private final List<Option> options = new ArrayList<>();
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private boolean exitIsLastNumber = false;

    public Menu(String title, String exitLabel, ConsoleInput input, ConsolePrinter printer) {
        this.title = title;
        this.exitLabel = exitLabel;
        this.input = input;
        this.printer = printer;
    }

    /** Adds an operation; after it runs the user presses ENTER to read the result. Returns this to chain calls. */
    public Menu add(String label, Runnable action) {
        options.add(new Option(label, action, true));
        return this;
    }

    /** Adds an option that opens another menu: no "press ENTER" pause, the sub-menu draws itself. */
    public Menu addSubmenu(String label, Runnable submenu) {
        options.add(new Option(label, submenu, false));
        return this;
    }

    /** The main menu of the assignment lists "Salir" as the last number (8) instead of 0. */
    public Menu exitAsLastNumber() {
        this.exitIsLastNumber = true;
        return this;
    }

    /** Draws the menu and runs the chosen actions until the user picks the exit/back option. */
    public void run() {
        List<String> labels = options.stream().map(Option::label).toList();
        int exitNumber = exitIsLastNumber ? options.size() + 1 : 0;
        int minNumber = exitIsLastNumber ? 1 : 0;
        int maxNumber = exitIsLastNumber ? options.size() + 1 : options.size();
        while (true) {
            printer.menuBox(title, labels, exitLabel, exitNumber);
            int choice = input.readInt("Seleccione una opción", minNumber, maxNumber);
            if (choice == exitNumber) {
                return;
            }
            execute(options.get(choice - 1));
        }
    }

    private void execute(Option option) {
        try {
            option.action().run();
        } catch (InputClosedException e) {
            throw e; // no more input: let Main close the program
        } catch (ErpException e) {
            // Expected problems (validation, business rule, not found, database): show the message
            printer.error(e.getMessage());
            AppLogger.getInstance().warning(Menu.class, "Operation \"" + option.label() + "\" failed: " + e.getMessage());
        } catch (RuntimeException e) {
            // Unexpected bug: log the stack trace and keep the program alive
            printer.error("Ocurrió un error inesperado. Revise logs/error_log.txt");
            AppLogger.getInstance().error(Menu.class, "Unexpected error in \"" + option.label() + "\"", e);
        }
        if (option.pauseAfter()) {
            input.pause();
        }
    }
}
