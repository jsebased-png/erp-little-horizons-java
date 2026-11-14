package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ParticipationReportController;
import com.littlehorizons.erp.controller.PerformanceReportController;
import com.littlehorizons.erp.dto.NamedCount;
import com.littlehorizons.erp.dto.StudentAverage;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screens of "7. Reportes y Estadísticas". The numbers come from the report controllers (they use Streams);
 * this view only prints them with bars. Reports that already exist in other modules are opened from here too.
 */
// SOLID: SRP – report screens only; DIP – reuses other views instead of copying their code
public class ReportView {

    private static final List<AcademicPeriod> REPORT_PERIODS = List.of(AcademicPeriod.I, AcademicPeriod.II,
            AcademicPeriod.III, AcademicPeriod.IV, AcademicPeriod.FINAL);
    private static final int BAR_WIDTH = 20;

    private final PerformanceReportController performance;
    private final ParticipationReportController participation;
    private final AttendanceView attendanceView;
    private final FinanceView financeView;
    private final ResourceView resourceView;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public ReportView(PerformanceReportController performance, ParticipationReportController participation,
                      AttendanceView attendanceView, FinanceView financeView, ResourceView resourceView,
                      ConsoleInput input, ConsolePrinter printer, PagedTable pagedTable) {
        this.performance = performance;
        this.participation = participation;
        this.attendanceView = attendanceView;
        this.financeView = financeView;
        this.resourceView = resourceView;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("REPORTES Y ESTADÍSTICAS", "Volver al menú principal", input, printer)
                .add("Rendimiento: ranking de estudiantes por período", this::showRanking)
                .add("Rendimiento: promedio por materia", this::showSubjectAverages)
                .add("Rendimiento: aprobados y reprobados", this::showPassFail)
                .add("Asistencia por materia (porcentaje)", this::showAttendanceBySubject)
                .add("Inasistencias preocupantes (más de 3 días en una semana)", attendanceView::showWorryingAbsences)
                .add("Morosos (pensión con más de 1 mes)", financeView::showDelinquents)
                .add("Recaudo por rango de fechas", financeView::showRevenue)
                .add("Participación en clubes", this::showClubParticipation)
                .add("Participación en eventos y podios", this::showEventParticipation)
                .add("Préstamos vencidos", resourceView::showOverdueLoans)
                .run();
    }

    // ------------------------------------------------------------------ academic performance

    private void showRanking() {
        int year = readYear();
        AcademicPeriod period = input.readChoice("Período", REPORT_PERIODS, AcademicPeriod::toString);
        int limit = input.readIntOrDefault("Cuántos estudiantes mostrar", 1, 100, 10);
        List<StudentAverage> ranking = performance.studentRanking(year, period, limit);
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            StudentAverage s = ranking.get(i);
            rows.add(List.of(String.valueOf(i + 1), s.studentName(), s.average().toPlainString(),
                    ViewFormat.bar(s.average().doubleValue() / 5.0, BAR_WIDTH)));
        }
        printer.section("Mejores promedios - período " + period + " - " + year);
        pagedTable.show(List.of("Puesto", "Estudiante", "Promedio", "Escala 0 - 5"), rows);
    }

    private void showSubjectAverages() {
        int year = readYear();
        Map<String, BigDecimal> averages = performance.subjectAverages(year);
        List<List<String>> rows = averages.entrySet().stream()
                .map(e -> List.of(e.getKey(), e.getValue().toPlainString(),
                        ViewFormat.bar(e.getValue().doubleValue() / 5.0, BAR_WIDTH)))
                .toList();
        printer.section("Promedio por materia - " + year);
        pagedTable.show(List.of("Materia", "Promedio", "Escala 0 - 5"), rows);
    }

    private void showPassFail() {
        int year = readYear();
        Map<Boolean, Long> counts = performance.passFailCounts(year);
        long passed = counts.getOrDefault(true, 0L);
        long failed = counts.getOrDefault(false, 0L);
        long total = passed + failed;
        double passedFraction = total == 0 ? 0 : (double) passed / total;
        printer.section("Notas aprobadas (" + PerformanceReportController.PASSING_SCORE + " o más) y reprobadas - " + year);
        printer.table(List.of("Resultado", "Notas", "Porcentaje", ""), List.of(
                List.of("Aprobadas", String.valueOf(passed), ViewFormat.percent(passedFraction),
                        ViewFormat.bar(passedFraction, BAR_WIDTH)),
                List.of("Reprobadas", String.valueOf(failed), ViewFormat.percent(total == 0 ? 0 : 1 - passedFraction),
                        ViewFormat.bar(total == 0 ? 0 : 1 - passedFraction, BAR_WIDTH))));
    }

    private void showAttendanceBySubject() {
        LocalDate to = input.readDateOrDefault("Fecha final", LocalDate.now());
        LocalDate from = input.readDateOrDefault("Fecha inicial", to.minusDays(30));
        Map<String, Double> rates = performance.attendanceRateBySubject(from, to);
        List<List<String>> rows = rates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> List.of(e.getKey(), ViewFormat.percent(e.getValue() / 100),
                        ViewFormat.bar(e.getValue() / 100, BAR_WIDTH)))
                .toList();
        printer.section("Asistencia por materia del " + ViewFormat.date(from) + " al " + ViewFormat.date(to));
        pagedTable.show(List.of("Materia", "Asistencia", ""), rows);
    }

    // ------------------------------------------------------------------ extracurricular

    private void showClubParticipation() {
        Map<String, Long> occupancy = participation.occupancyPercentPerClub();
        List<List<String>> rows = participation.membersPerClub().stream()
                .map(c -> {
                    long percent = occupancy.getOrDefault(c.name(), 0L);
                    return List.of(c.name(), String.valueOf(c.count()), percent + "%",
                            ViewFormat.bar(percent / 100.0, BAR_WIDTH));
                })
                .toList();
        printer.section("Miembros y ocupación por club");
        pagedTable.show(List.of("Club", "Miembros", "Ocupación", ""), rows);

        printer.section("Estudiantes en más clubes");
        printer.table(List.of("Estudiante", "Clubes"), toRows(participation.studentsWithMostClubs(5)));
    }

    private void showEventParticipation() {
        printer.section("Participantes por evento");
        pagedTable.show(List.of("Evento", "Participantes"), toRows(participation.participantsPerEvent()));

        Map<EventType, Long> byType = participation.participationByEventType();
        long max = byType.values().stream().max(Long::compare).orElse(1L);
        List<List<String>> typeRows = byType.entrySet().stream()
                .map(e -> List.of(e.getKey().toString(), String.valueOf(e.getValue()),
                        ViewFormat.bar((double) e.getValue() / max, BAR_WIDTH)))
                .toList();
        printer.section("Participaciones por tipo de evento");
        printer.table(List.of("Tipo", "Participaciones", ""), typeRows);

        printer.section("Estudiantes con más podios (1er, 2do o 3er lugar)");
        printer.table(List.of("Estudiante", "Podios"), toRows(participation.podiumRanking(5)));
    }

    private List<List<String>> toRows(List<NamedCount> counts) {
        return counts.stream().map(c -> List.of(c.name(), String.valueOf(c.count()))).toList();
    }

    private int readYear() {
        return input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());
    }
}
