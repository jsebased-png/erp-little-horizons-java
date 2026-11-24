package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.PerformanceReportController;
import com.littlehorizons.erp.dto.SubjectAverage;
import com.littlehorizons.erp.dto.SubjectPerformanceReport;
import com.littlehorizons.erp.view.console.ConsolePrinter;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Screen "Reporte de Rendimiento Académico por Materia".
 * Flow: AcademicView (menu option) -> this view -> PerformanceReportController (Streams) -> AcademicRecordDao -> MySQL.
 * It receives the controller through its constructor (MainView takes it from AppContext) and only prints:
 * every number is already calculated and ordered; the controller also saves the report as a .txt file. The "press ENTER" pause and the error handling are done by
 * Menu.add(...), so this class needs no try/catch.
 */
// SOLID: SRP – prints one report; DIP – depends on the controller, never on a DAO
public class SubjectPerformanceView {

    private final PerformanceReportController performance;
    private final ConsolePrinter printer;

    public SubjectPerformanceView(PerformanceReportController performance, ConsolePrinter printer) {
        this.performance = performance;
        this.printer = printer;
    }

    public void showReport() {
        SubjectPerformanceReport report = performance.subjectPerformanceReport();
        printer.section("REPORTE DE RENDIMIENTO ACADÉMICO POR MATERIA");

        if (report.isEmpty()) {
            printer.warning("No hay registros académicos suficientes para generar estadísticas.");
            return;
        }

        printer.println("  Promedio general de todas las notas registradas, de mayor a menor.");
        printer.table(List.of("Código", "Materia", "Promedio"),
                report.ranking().stream()
                        .map(s -> List.of(s.subject().code(), s.subject().name(), formatAverage(s)))
                        .toList());

        printTopSubjects(report.topSubjects());

        // every generated report is also saved as a .txt file (the path is shown so the user can find it)
        printer.success("Reporte guardado en: " + performance.exportSubjectPerformanceReport(report).toAbsolutePath());
    }

    /** One top subject is shown as the featured subject; several mean a tie and all of them are listed. */
    private void printTopSubjects(List<SubjectAverage> topSubjects) {
        printer.section("Materia(s) con Mejor Rendimiento");
        if (topSubjects.size() == 1) {
            SubjectAverage best = topSubjects.get(0);
            printer.success("Materia destacada: " + describe(best));
            return;
        }
        printer.info(topSubjects.size() + " materias comparten el mejor rendimiento (promedio "
                + formatAverage(topSubjects.get(0)) + "):");
        printer.println(topSubjects.stream()
                .map(s -> "    * " + describe(s))
                .collect(Collectors.joining(System.lineSeparator())));
    }

    private String describe(SubjectAverage s) {
        return s.subject().code() + " - " + s.subject().name() + " (" + formatAverage(s) + ")";
    }

    /** Locale.US guarantees a dot as decimal separator ("4.5"), whatever the language of the operating system. */
    private String formatAverage(SubjectAverage s) {
        return String.format(Locale.US, "%.1f", s.average());
    }
}
