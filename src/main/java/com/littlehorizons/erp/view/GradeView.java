package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.GradeController;
import com.littlehorizons.erp.dto.HistoryLine;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens for grades: register a score, averages (subject, period, course), report card and academic history.
 * The averages are calculated by GradeController; this view only prints them.
 */
// SOLID: SRP – grade screens only
public class GradeView {

    private static final List<AcademicPeriod> SCORE_PERIODS = List.of(AcademicPeriod.I, AcademicPeriod.II,
            AcademicPeriod.III, AcademicPeriod.IV, AcademicPeriod.FINAL);
    private static final List<AcademicPeriod> TERM_PERIODS = SCORE_PERIODS.subList(0, 4);

    private final GradeController grades;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public GradeView(GradeController grades, NameLookup names, Pickers pickers, ConsoleInput input,
                     ConsolePrinter printer, PagedTable pagedTable) {
        this.grades = grades;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("NOTAS Y PROMEDIOS", "Volver", input, printer)
                .add("Registrar o corregir una nota", this::registerScore)
                .add("Boletín del estudiante por año", this::reportCard)
                .add("Promedio de un estudiante en una materia", this::subjectAverage)
                .add("Promedio general del estudiante por período", this::periodAverages)
                .add("Promedio del curso en una materia", this::courseAverage)
                .add("Historial académico completo", this::history)
                .run();
    }

    private void registerScore() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Optional<SubjectEnrollment> enrolled = pickers.enrolledSubject(student.get().getId(), year);
        if (enrolled.isEmpty()) {
            return;
        }
        printer.info("Seleccione el profesor que registra la nota (debe estar asignado a la materia).");
        Optional<Teacher> teacher = pickers.teacher();
        if (teacher.isEmpty()) {
            return;
        }
        AcademicPeriod period = input.readChoice("Período", SCORE_PERIODS, AcademicPeriod::toString);
        BigDecimal score = input.readDecimal("Nota (0.0 a 5.0, un decimal)", 0, 5);
        String observations = input.readOptionalText("Observaciones");

        AcademicRecord record = grades.registerScore(student.get().getId(), enrolled.get().getSubjectId(),
                teacher.get().getId(), year, period, score, observations);
        printer.success("Nota " + ViewFormat.score(record.getScore()) + " guardada en "
                + NameLookup.of(names.subjects(), record.getSubjectId()) + ", período " + period + ".");
    }

    /** One row per subject with the five scores and the average of periods I-IV. */
    private void reportCard() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Map<String, List<AcademicRecord>> bySubject = grades.recordsBySubject(student.get().getId(), year);
        List<List<String>> rows = bySubject.entrySet().stream()
                .map(entry -> {
                    List<AcademicRecord> records = entry.getValue();
                    int subjectId = records.get(0).getSubjectId();
                    String average = grades.subjectAverage(student.get().getId(), subjectId, year)
                            .map(BigDecimal::toPlainString).orElse("-");
                    return List.of(entry.getKey(),
                            scoreOf(records, AcademicPeriod.I), scoreOf(records, AcademicPeriod.II),
                            scoreOf(records, AcademicPeriod.III), scoreOf(records, AcademicPeriod.IV),
                            scoreOf(records, AcademicPeriod.FINAL), average);
                })
                .toList();
        printer.section("Boletín de " + student.get().getFullName() + " - " + year);
        printer.table(List.of("Materia", "I", "II", "III", "IV", "Final", "Promedio"), rows);
    }

    /** Looks for the score of one period inside the list of records of a subject. */
    private String scoreOf(List<AcademicRecord> records, AcademicPeriod period) {
        return records.stream()
                .filter(r -> r.getPeriod() == period)
                .findFirst()
                .map(r -> ViewFormat.score(r.getScore()))
                .orElse("-");
    }

    private void subjectAverage() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Optional<SubjectEnrollment> enrolled = pickers.enrolledSubject(student.get().getId(), year);
        if (enrolled.isEmpty()) {
            return;
        }
        String subjectName = NameLookup.of(names.subjects(), enrolled.get().getSubjectId());
        grades.subjectAverage(student.get().getId(), enrolled.get().getSubjectId(), year)
                .ifPresentOrElse(
                        avg -> printer.success("Promedio en " + subjectName + " (períodos I a IV): " + avg),
                        () -> printer.info("Todavía no hay notas registradas en " + subjectName + "."));
    }

    private void periodAverages() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        List<List<String>> rows = TERM_PERIODS.stream()
                .map(period -> List.of("Período " + period,
                        grades.periodAverage(student.get().getId(), year, period)
                                .map(BigDecimal::toPlainString).orElse("-")))
                .toList();
        printer.section("Promedio general de " + student.get().getFullName() + " - " + year);
        printer.table(List.of("Período", "Promedio general"), rows);
    }

    private void courseAverage() {
        Optional<Subject> subject = pickers.subjectOfAnyGrade();
        if (subject.isEmpty()) {
            return;
        }
        int year = readYear();
        Map<AcademicPeriod, BigDecimal> byPeriod = grades.courseAverageByPeriod(subject.get().getId(), year);
        List<List<String>> rows = byPeriod.entrySet().stream()
                .map(e -> List.of("Período " + e.getKey(), e.getValue().toPlainString()))
                .toList();
        printer.section("Promedio del curso: " + subject.get().getName() + " - " + year);
        printer.table(List.of("Período", "Promedio del curso"), rows);
        grades.courseAverage(subject.get().getId(), year)
                .ifPresent(avg -> printer.info("Promedio general de la materia: " + avg));
    }

    private void history() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        List<HistoryLine> lines = grades.history(student.get().getId());
        List<List<String>> rows = lines.stream()
                .map(l -> List.of(String.valueOf(l.academicYear()), l.subjectName(), l.period().toString(),
                        ViewFormat.score(l.score())))
                .toList();
        printer.section("Historial académico de " + student.get().getFullName());
        pagedTable.show(List.of("Año", "Materia", "Período", "Nota"), rows);
    }

    private int readYear() {
        return input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());
    }
}
