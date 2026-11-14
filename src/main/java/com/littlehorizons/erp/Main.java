package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppContext;
import com.littlehorizons.erp.config.DatabaseConnection;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.view.MainView;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Application entry point. It prepares the console (UTF-8 so the box characters and accents display correctly),
 * checks the database connection, builds the controllers (AppContext) and opens the main menu.
 * It contains no business logic.
 */
public class Main {

    public static void main(String[] args) {
        AppLogger logger = AppLogger.getInstance();
        ConsolePrinter printer = new ConsolePrinter(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        ConsoleInput input = new ConsoleInput(new Scanner(System.in, StandardCharsets.UTF_8), printer);

        try {
            logger.info(Main.class, "Application starting");
            if (!DatabaseConnection.getInstance().isReachable()) {
                printer.error("No se pudo conectar a la base de datos. Revise config.properties y que MySQL esté activo.");
                printer.info("El detalle está en logs/error_log.txt");
                return;
            }
            logger.info(Main.class, "Database connection OK");
            new MainView(new AppContext(), input, printer).run();
            logger.info(Main.class, "Application closed by the user");
        } catch (InputClosedException e) {
            logger.info(Main.class, "Input stream closed; application ends");
        } catch (ErpException e) {
            logger.error(Main.class, "Startup failed", e);
            printer.error("No se pudo iniciar la aplicación: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error(Main.class, "Unexpected failure", e);
            printer.error("Error inesperado. Revise logs/error_log.txt");
        }
    }
}
