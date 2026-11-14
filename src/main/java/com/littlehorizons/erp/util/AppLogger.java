package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.FileOperationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes application events to logs/error_log.txt.
 * It exists so errors are traceable after the app closes.
 * Line format: [timestamp] [LEVEL] [ClassName] message (+ stack trace).
 */
// SOLID: SRP – this class only formats and writes log lines, nothing else
public final class AppLogger {

    // Design pattern: Singleton – one logger, one file, no concurrent writers
    private static final AppLogger INSTANCE = new AppLogger();

    private static final Path LOG_DIR = Paths.get("logs");
    private static final Path LOG_FILE = LOG_DIR.resolve("error_log.txt");
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {
    }

    public static AppLogger getInstance() {
        return INSTANCE;
    }

    public void info(Class<?> source, String message) {
        write("INFO", source, message, null);
    }

    public void warning(Class<?> source, String message) {
        write("WARNING", source, message, null);
    }

    public void error(Class<?> source, String message, Throwable cause) {
        write("ERROR", source, message, cause);
    }

    /** synchronized: two threads must never interleave lines. */
    private synchronized void write(String level, Class<?> source, String message, Throwable cause) {
        StringBuilder line = new StringBuilder()
                .append('[').append(LocalDateTime.now().format(TIMESTAMP)).append("] ")
                .append('[').append(level).append("] ")
                .append('[').append(source.getSimpleName()).append("] ")
                .append(message).append(System.lineSeparator());

        if (cause != null) {
            line.append(stackTraceOf(cause));
        }

        try {
            Files.createDirectories(LOG_DIR);
            Files.writeString(LOG_FILE, line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // The logger must never crash the app: report to stderr only.
            System.err.println("Could not write log: " + e.getMessage());
        }
    }

    private String stackTraceOf(Throwable cause) {
        StringWriter buffer = new StringWriter();
        try (PrintWriter printer = new PrintWriter(buffer)) {
            cause.printStackTrace(printer);
        }
        return buffer.toString();
    }
}
