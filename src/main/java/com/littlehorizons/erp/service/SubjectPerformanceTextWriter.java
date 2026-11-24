package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dto.SubjectAverage;
import com.littlehorizons.erp.dto.SubjectPerformanceReport;
import com.littlehorizons.erp.exception.FileOperationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Saves the "performance by subject" report as a plain UTF-8 .txt file, one new file per report
 * (rendimiento_materias_yyyyMMdd_HHmmss.txt) inside the folder given by reports.dir in config.properties.
 * Flow: SubjectPerformanceView -> PerformanceReportController -> this class -> file system.
 * It only formats and writes what the controller already calculated; it never queries the database.
 * Like TextInvoiceGenerator, it wraps IOException in FileOperationException so upper layers do not depend on java.io.
 */
// SOLID: SRP – file output only; DIP – AppContext creates it and injects the folder
public class SubjectPerformanceTextWriter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter HEADER_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path reportsDir;

    public SubjectPerformanceTextWriter(Path reportsDir) {
        this.reportsDir = reportsDir;
    }

    /**
     * Creates the folder if missing and writes the report. Files.writeString opens and closes the file by itself
     * (the same guarantee as try-with-resources).
     *
     * @return the path of the created file
     * @throws FileOperationException if the file cannot be written
     */
    public Path write(SubjectPerformanceReport report) {
        LocalDateTime now = LocalDateTime.now();
        Path target = reportsDir.resolve("rendimiento_materias_" + now.format(FILE_STAMP) + ".txt");
        try {
            Files.createDirectories(reportsDir);
            Files.writeString(target, render(report, now), StandardCharsets.UTF_8);
            return target;
        } catch (IOException e) {
            throw new FileOperationException("Could not write the report file: " + target, e);
        }
    }

    /** Builds the whole text with Streams: ranking table first, then the best subject(s). */
    private String render(SubjectPerformanceReport report, LocalDateTime generatedAt) {
        // column widths = longest value of each column (never smaller than its header)
        int codeWidth = Math.max("Código".length(), report.ranking().stream()
                .mapToInt(s -> s.subject().code().length()).max().orElse(0));
        int nameWidth = Math.max("Materia".length(), report.ranking().stream()
                .mapToInt(s -> s.subject().name().length()).max().orElse(0));
        String rowFormat = "%-" + codeWidth + "s | %-" + nameWidth + "s | %s";

        Stream<String> table = Stream.concat(
                Stream.of(String.format(rowFormat, "Código", "Materia", "Promedio")),
                report.ranking().stream().map(s ->
                        String.format(rowFormat, s.subject().code(), s.subject().name(), average(s))));

        List<SubjectAverage> top = report.topSubjects();
        String topTitle = top.size() == 1
                ? "Materia destacada:"
                : top.size() + " materias comparten el mejor rendimiento:";

        return Stream.of(
                        Stream.of("REPORTE DE RENDIMIENTO ACADÉMICO POR MATERIA",
                                "Generado: " + generatedAt.format(HEADER_STAMP),
                                "Promedio general de todas las notas registradas, de mayor a menor.",
                                ""),
                        table,
                        Stream.of("", "MATERIA(S) CON MEJOR RENDIMIENTO", topTitle),
                        top.stream().map(s -> "  * " + s.subject().code() + " - " + s.subject().name()
                                + " (" + average(s) + ")"))
                .flatMap(lines -> lines)
                .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
    }

    /** Locale.US keeps the dot as decimal separator ("4.5") on any operating system. */
    private String average(SubjectAverage s) {
        return String.format(Locale.US, "%.1f", s.average());
    }
}
