package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.dto.AbsenceAlert;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens for daily attendance: register, view per student and the worrying-absences report.
 */
// SOLID: SRP – attendance screens only
public class AttendanceView {

    private final AttendanceController attendance;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public AttendanceView(AttendanceController attendance, NameLookup names, Pickers pickers, ConsoleInput input,
                          ConsolePrinter printer, PagedTable pagedTable) {
        this.attendance = attendance;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("ASISTENCIA", "Volver", input, printer)
                .add("Registrar asistencia de un estudiante", this::register)
                .add("Ver asistencia de un estudiante", this::showStudentAttendance)
                .add("Reporte de inasistencias preocupantes", this::showWorryingAbsences)
                .run();
    }

    private void register() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        LocalDate date = input.readDateOrDefault("Fecha de la clase", LocalDate.now());
        Optional<SubjectEnrollment> enrolled = pickers.enrolledSubject(student.get().getId(), date.getYear());
        if (enrolled.isEmpty()) {
            return;
        }
        AttendanceStatus status = input.readEnum("Estado de asistencia", AttendanceStatus.class);
        String observations = input.readOptionalText("Observaciones");
        Integer teacherId = null;
        if (input.confirm("¿Registrar qué profesor tomó la asistencia?")) {
            teacherId = pickers.teacher().map(Teacher::getId).orElse(null);
        }
        attendance.register(student.get().getId(), enrolled.get().getSubjectId(), date, status, observations, teacherId);
        printer.success("Asistencia guardada: " + status + " el " + ViewFormat.date(date) + ".");
    }

    private void showStudentAttendance() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> subjectNames = names.subjects();
        List<Attendance> records = attendance.listByStudent(student.get().getId());
        List<List<String>> rows = records.stream()
                .map(a -> List.of(ViewFormat.date(a.getDate()), NameLookup.of(subjectNames, a.getSubjectId()),
                        a.getStatus().toString(), ViewFormat.text(a.getObservations())))
                .toList();
        printer.section("Asistencia de " + student.get().getFullName());
        pagedTable.show(List.of("Fecha", "Materia", "Estado", "Observaciones"), rows);
        attendance.attendanceRate(student.get().getId())
                .ifPresent(rate -> printer.info("Porcentaje de asistencia: " + ViewFormat.percent(rate / 100)
                        + "  " + ViewFormat.bar(rate / 100, 20)));
    }

    /** Public because the reports menu reuses this same screen. */
    public void showWorryingAbsences() {
        LocalDate to = input.readDateOrDefault("Fecha final", LocalDate.now());
        LocalDate from = input.readDateOrDefault("Fecha inicial", to.minusDays(30));
        List<AbsenceAlert> alerts = attendance.findWorryingAbsences(from, to);
        List<List<String>> rows = alerts.stream()
                .map(a -> List.of(a.studentName(), "Semana del " + ViewFormat.date(a.weekStart()),
                        String.valueOf(a.absentDays())))
                .toList();
        printer.section("Estudiantes con más de " + AttendanceController.ABSENT_DAYS_THRESHOLD
                + " días ausente en una semana");
        printer.table(List.of("Estudiante", "Semana", "Días ausente"), rows);
    }
}
