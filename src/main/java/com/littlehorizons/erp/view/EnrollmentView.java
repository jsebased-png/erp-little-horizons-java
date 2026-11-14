package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
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
 * Screens for grade enrollment (matrícula) and subject enrollment (inscripción en materias).
 */
// SOLID: SRP – enrollment screens only
public class EnrollmentView {

    /** Subjects are enrolled by the four school periods (Final and Anual do not apply). */
    private static final List<AcademicPeriod> TERM_PERIODS =
            List.of(AcademicPeriod.I, AcademicPeriod.II, AcademicPeriod.III, AcademicPeriod.IV);

    private final EnrollmentController enrollments;
    private final SubjectController subjects;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public EnrollmentView(EnrollmentController enrollments, SubjectController subjects, NameLookup names,
                          Pickers pickers, ConsoleInput input, ConsolePrinter printer, PagedTable pagedTable) {
        this.enrollments = enrollments;
        this.subjects = subjects;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("MATRÍCULAS E INSCRIPCIONES", "Volver", input, printer)
                .add("Matricular estudiante en un grado", this::enrollInGrade)
                .add("Retirar matrícula de un estudiante", this::withdrawFromGrade)
                .add("Ver matrículas de un estudiante", this::showStudentEnrollments)
                .add("Ver matrículas de un año", this::showYearEnrollments)
                .add("Inscribir estudiante en una materia", this::enrollInSubject)
                .add("Inscribir estudiante en TODAS las materias de su grado", this::enrollInAllSubjects)
                .add("Retirar estudiante de una materia", this::withdrawFromSubject)
                .add("Ver materias inscritas de un estudiante", this::showSubjectEnrollments)
                .run();
    }

    // ------------------------------------------------------------------ grade enrollment

    private void enrollInGrade() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Optional<Grade> grade = pickers.grade();
        if (grade.isEmpty()) {
            return;
        }
        int year = readYear();
        String notes = input.readOptionalText("Observaciones");
        Enrollment saved = enrollments.enrollInGrade(student.get().getId(), grade.get().getId(), year, notes);
        printer.success("Matrícula registrada (ID " + saved.getId() + "): " + student.get().getFullName()
                + " en " + grade.get().getName() + ", año " + year + ".");
    }

    private void withdrawFromGrade() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        List<Enrollment> active = enrollments.listEnrollmentsByStudent(student.get().getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();
        Map<Integer, String> gradeNames = names.grades();
        Optional<Enrollment> picked = pickers.choose("Matrículas activas", active, List.of("Año", "Grado"),
                e -> List.of(String.valueOf(e.getAcademicYear()), NameLookup.of(gradeNames, e.getGradeId())),
                Enrollment::getId);
        if (picked.isPresent() && input.confirm("¿Retirar esta matrícula?")) {
            enrollments.withdrawFromGrade(picked.get().getId());
            printer.success("Matrícula retirada.");
        }
    }

    private void showStudentEnrollments() {
        pickers.student().ifPresent(s -> showEnrollments(enrollments.listEnrollmentsByStudent(s.getId())));
    }

    private void showYearEnrollments() {
        int year = readYear();
        showEnrollments(enrollments.listEnrollmentsByYear(year));
    }

    private void showEnrollments(List<Enrollment> list) {
        Map<Integer, String> gradeNames = names.grades();
        Map<Integer, String> studentNames = names.students();
        List<List<String>> rows = list.stream()
                .map(e -> List.of(String.valueOf(e.getId()), NameLookup.of(studentNames, e.getStudentId()),
                        NameLookup.of(gradeNames, e.getGradeId()), String.valueOf(e.getAcademicYear()),
                        e.getStatus().toString(), ViewFormat.date(e.getEnrollmentDate()),
                        ViewFormat.date(e.getWithdrawalDate())))
                .toList();
        pagedTable.show(List.of("ID", "Estudiante", "Grado", "Año", "Estado", "Matriculado", "Retirado"), rows);
    }

    // ------------------------------------------------------------------ subject enrollment

    private void enrollInSubject() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Optional<Enrollment> gradeEnrollment = enrollments.findActiveEnrollment(student.get().getId(), year);
        if (gradeEnrollment.isEmpty()) {
            printer.warning("El estudiante no tiene matrícula activa en " + year + ". Matricúlelo primero en un grado.");
            return;
        }
        // Only the subjects of the student's grade are offered (the controller checks this rule again)
        List<Subject> gradeSubjects = subjects.listSubjectsByGrade(gradeEnrollment.get().getGradeId());
        Optional<Subject> subject = pickers.subjectFrom(gradeSubjects);
        if (subject.isEmpty()) {
            return;
        }
        AcademicPeriod period = input.readChoice("Período", TERM_PERIODS, AcademicPeriod::toString);
        String section = input.readOptionalText("Sección (ej. A)");
        enrollments.enrollInSubject(student.get().getId(), subject.get().getId(), year, period, section);
        printer.success("Inscrito en " + subject.get().getName() + ", período " + period + ".");
    }

    private void enrollInAllSubjects() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        AcademicPeriod period = input.readChoice("Período", TERM_PERIODS, AcademicPeriod::toString);
        int created = enrollments.enrollInAllGradeSubjects(student.get().getId(), year, period);
        if (created == 0) {
            printer.info("El estudiante ya estaba inscrito en todas las materias de ese período.");
        } else {
            printer.success("Se crearon " + created + " inscripciones en materias.");
        }
    }

    private void withdrawFromSubject() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Map<Integer, String> subjectNames = names.subjects();
        List<SubjectEnrollment> active =
                enrollments.listSubjectEnrollments(student.get().getId(), year).stream()
                        .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                        .toList();
        Optional<SubjectEnrollment> picked = pickers.choose("Inscripciones activas", active,
                List.of("Materia", "Período"),
                e -> List.of(NameLookup.of(subjectNames, e.getSubjectId()), e.getPeriod().toString()),
                SubjectEnrollment::getId);
        if (picked.isPresent() && input.confirm("¿Retirar al estudiante de esa materia?")) {
            enrollments.withdrawFromSubject(picked.get().getId());
            printer.success("Inscripción retirada.");
        }
    }

    private void showSubjectEnrollments() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        int year = readYear();
        Map<Integer, String> subjectNames = names.subjects();
        List<List<String>> rows = enrollments.listSubjectEnrollments(student.get().getId(), year).stream()
                .map(e -> List.of(String.valueOf(e.getId()), NameLookup.of(subjectNames, e.getSubjectId()),
                        e.getPeriod().toString(), ViewFormat.text(e.getSection()), e.getStatus().toString(),
                        ViewFormat.date(e.getEnrollmentDate())))
                .toList();
        pagedTable.show(List.of("ID", "Materia", "Período", "Sección", "Estado", "Fecha"), rows);
    }

    private int readYear() {
        return input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());
    }
}
