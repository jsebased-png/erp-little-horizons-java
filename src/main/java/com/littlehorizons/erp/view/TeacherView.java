package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.ScheduleSlot;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.model.WeekDay;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Screens of "3. Gestión de Profesores": directory, subject assignments and weekly schedules.
 * Schedule conflicts are detected by TeacherController; the view only shows the message it throws.
 */
// SOLID: SRP – teacher screens only
public class TeacherView {

    private final TeacherController teachers;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public TeacherView(TeacherController teachers, NameLookup names, Pickers pickers, ConsoleInput input,
                       ConsolePrinter printer, PagedTable pagedTable) {
        this.teachers = teachers;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("GESTIÓN DE PROFESORES", "Volver al menú principal", input, printer)
                .add("Registrar profesor", this::register)
                .add("Listar profesores", this::list)
                .add("Editar profesor", this::edit)
                .add("Cambiar estado (Activo / Inactivo / Licencia)", this::changeStatus)
                .add("Asignar materia a un profesor", this::assignSubject)
                .add("Ver asignaciones y horarios de un profesor", this::showAssignments)
                .add("Quitar asignación de materia", this::removeAssignment)
                .add("Agregar horario a una asignación", this::addSlot)
                .add("Quitar un horario", this::removeSlot)
                .run();
    }

    // ------------------------------------------------------------------ directory

    private void register() {
        Teacher teacher = new Teacher();
        // A teacher is an adult, so TI (identity card for minors) is not offered
        teacher.setDocumentType(input.readChoice("Tipo de documento",
                List.of(DocumentType.CC, DocumentType.CE, DocumentType.PASSPORT), DocumentType::toString));
        teacher.setDocumentNumber(input.readDocument(teacher.getDocumentType(), "Número de documento"));
        teacher.setFirstName(input.readPersonName("Primer nombre"));
        teacher.setMiddleName(input.readOptionalPersonName("Segundo nombre"));
        teacher.setFirstSurname(input.readPersonName("Primer apellido"));
        teacher.setSecondSurname(input.readOptionalPersonName("Segundo apellido"));
        teacher.setEmail(input.readEmail("Correo electrónico"));
        teacher.setPhone(input.readOptionalPhone("Teléfono"));
        teacher.setSpecialization(input.readOptionalText("Especialización (ej. Matemáticas)"));
        teacher.setHireDate(input.readDateOrDefault("Fecha de contratación", LocalDate.now()));
        Teacher saved = teachers.registerTeacher(teacher);
        printer.success("Profesor registrado con ID " + saved.getId() + ": " + saved.getFullName());
    }

    private void list() {
        List<List<String>> rows = teachers.listTeachers().stream()
                .map(t -> List.of(String.valueOf(t.getId()), t.getDocumentNumber(), t.getFullName(),
                        ViewFormat.text(t.getSpecialization()), t.getEmail(), t.getStatus().toString()))
                .toList();
        pagedTable.show(List.of("ID", "Documento", "Nombre", "Especialización", "Correo", "Estado"), rows);
    }

    private void edit() {
        Optional<Teacher> picked = pickers.teacher();
        if (picked.isEmpty()) {
            return;
        }
        Teacher teacher = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        teacher.setFirstName(input.readPersonNameOrKeep("Primer nombre", teacher.getFirstName()));
        teacher.setMiddleName(input.readPersonNameOrKeep("Segundo nombre", teacher.getMiddleName()));
        teacher.setFirstSurname(input.readPersonNameOrKeep("Primer apellido", teacher.getFirstSurname()));
        teacher.setSecondSurname(input.readPersonNameOrKeep("Segundo apellido", teacher.getSecondSurname()));
        teacher.setEmail(input.readEmailOrKeep("Correo electrónico", teacher.getEmail()));
        teacher.setPhone(input.readPhoneOrKeep("Teléfono", teacher.getPhone()));
        teacher.setSpecialization(input.readTextOrKeep("Especialización", teacher.getSpecialization()));
        teachers.updateTeacher(teacher);
        printer.success("Profesor actualizado.");
    }

    private void changeStatus() {
        Optional<Teacher> picked = pickers.teacher();
        if (picked.isEmpty()) {
            return;
        }
        TeacherStatus status = input.readEnum("Nuevo estado", TeacherStatus.class);
        teachers.changeStatus(picked.get().getId(), status);
        printer.success(picked.get().getFullName() + " ahora está: " + status + ".");
    }

    // ------------------------------------------------------------------ assignments

    private void assignSubject() {
        Optional<Teacher> teacher = pickers.teacher();
        if (teacher.isEmpty()) {
            return;
        }
        Optional<Subject> subject = pickers.subjectOfAnyGrade();
        if (subject.isEmpty()) {
            return;
        }
        int year = readYear();
        String section = input.readText("Sección (ej. A)");
        teachers.assignSubject(teacher.get().getId(), subject.get().getId(), year, section);
        printer.success(teacher.get().getFullName() + " asignado a " + subject.get().getName() + " (" + year + ").");
    }

    private void showAssignments() {
        Optional<Teacher> teacher = pickers.teacher();
        if (teacher.isEmpty()) {
            return;
        }
        int year = readYear();
        Map<Integer, String> subjectNames = names.subjects();
        List<List<String>> rows = teachers.listAssignments(teacher.get().getId(), year).stream()
                .map(a -> List.of(String.valueOf(a.getId()), NameLookup.of(subjectNames, a.getSubjectId()),
                        a.getSection(), scheduleText(a.getId())))
                .toList();
        printer.section("Asignaciones de " + teacher.get().getFullName() + " - " + year);
        printer.table(List.of("ID", "Materia", "Sección", "Horario"), rows);
    }

    private void removeAssignment() {
        Optional<TeacherAssignment> picked = chooseAssignment();
        if (picked.isPresent() && input.confirm("¿Quitar esta asignación?")) {
            teachers.removeAssignment(picked.get().getId());
            printer.success("Asignación eliminada.");
        }
    }

    // ------------------------------------------------------------------ schedule

    private void addSlot() {
        Optional<TeacherAssignment> assignment = chooseAssignment();
        if (assignment.isEmpty()) {
            return;
        }
        WeekDay day = input.readEnum("Día de la semana", WeekDay.class);
        LocalTime start = input.readTime("Hora de inicio");
        LocalTime end = input.readTime("Hora de fin");
        teachers.addScheduleSlot(assignment.get().getId(), day, start, end);
        printer.success("Horario agregado: " + day + " " + start + " - " + end + ".");
    }

    private void removeSlot() {
        Optional<TeacherAssignment> assignment = chooseAssignment();
        if (assignment.isEmpty()) {
            return;
        }
        Optional<ScheduleSlot> slot = pickers.choose("Horarios de la asignación",
                teachers.listSlots(assignment.get().getId()), List.of("Día", "Inicio", "Fin"),
                s -> List.of(s.getDay().toString(), s.getStartTime().toString(), s.getEndTime().toString()),
                ScheduleSlot::getId);
        if (slot.isPresent() && input.confirm("¿Quitar este horario?")) {
            teachers.removeScheduleSlot(slot.get().getId());
            printer.success("Horario eliminado.");
        }
    }

    /** Asks for a teacher and a year, then lets the user choose one of the teacher's assignments. */
    private Optional<TeacherAssignment> chooseAssignment() {
        Optional<Teacher> teacher = pickers.teacher();
        if (teacher.isEmpty()) {
            return Optional.empty();
        }
        int year = readYear();
        Map<Integer, String> subjectNames = names.subjects();
        return pickers.choose("Asignaciones de " + teacher.get().getFullName(),
                teachers.listAssignments(teacher.get().getId(), year), List.of("Materia", "Sección"),
                a -> List.of(NameLookup.of(subjectNames, a.getSubjectId()), a.getSection()),
                TeacherAssignment::getId);
    }

    /** "Lunes 07:00-08:00; Martes 09:00-10:00" – joins the slots of an assignment into one text. */
    private String scheduleText(int assignmentId) {
        String text = teachers.listSlots(assignmentId).stream()
                .map(s -> s.getDay() + " " + s.getStartTime() + "-" + s.getEndTime())
                .collect(Collectors.joining("; "));
        return text.isEmpty() ? "Sin horario" : text;
    }

    private int readYear() {
        return input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());
    }
}
