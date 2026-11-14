package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * "Pick one" dialogs shared by all the views: the user sees a list (or searches a student) and chooses by id.
 * Because the user can only choose from what was shown, the ids that reach the controllers already exist.
 * Every method returns an empty Optional when the user cancels, so the calling view simply stops.
 */
// SOLID: SRP – selection dialogs only; reused by every view instead of repeating this code in each one
public class Pickers {

    private static final int MAX_SEARCH_RESULTS = 20;

    private final StudentController students;
    private final SubjectController subjects;
    private final TeacherController teachers;
    private final ClubController clubs;
    private final EventController events;
    private final EnrollmentController enrollments;
    private final NameLookup names;
    private final ConsoleInput input;
    private final ConsolePrinter printer;

    public Pickers(StudentController students, SubjectController subjects, TeacherController teachers,
                   ClubController clubs, EventController events, EnrollmentController enrollments,
                   NameLookup names, ConsoleInput input, ConsolePrinter printer) {
        this.students = students;
        this.subjects = subjects;
        this.teachers = teachers;
        this.clubs = clubs;
        this.events = events;
        this.enrollments = enrollments;
        this.names = names;
        this.input = input;
        this.printer = printer;
    }

    // ------------------------------------------------------------------ generic list chooser

    /**
     * Prints the items in a table (first column = id) and asks for an id. 0 cancels.
     * The id must be one of the printed ones.
     */
    public <T> Optional<T> choose(String heading, List<T> items, List<String> headers,
                                  Function<T, List<String>> toRow, ToIntFunction<T> idOf) {
        if (items.isEmpty()) {
            printer.info("No hay registros disponibles para seleccionar.");
            return Optional.empty();
        }
        List<String> fullHeaders = new ArrayList<>();
        fullHeaders.add("ID");
        fullHeaders.addAll(headers);
        List<List<String>> rows = items.stream().map(item -> {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(idOf.applyAsInt(item)));
            row.addAll(toRow.apply(item));
            return row;
        }).toList();

        printer.section(heading);
        printer.table(fullHeaders, rows);
        while (true) {
            int id = input.readInt("ID a seleccionar (0 = cancelar)", 0, Integer.MAX_VALUE);
            if (id == 0) {
                return Optional.empty();
            }
            Optional<T> found = items.stream().filter(item -> idOf.applyAsInt(item) == id).findFirst();
            if (found.isPresent()) {
                return found;
            }
            printer.warning("Ese ID no está en la lista mostrada.");
        }
    }

    // ------------------------------------------------------------------ students

    /** Searches by document or by part of the name; if several match, the user chooses one. */
    public Optional<Student> student() {
        while (true) {
            String text = input.readLine("Documento o nombre del estudiante (ENTER = cancelar)");
            if (text.isEmpty()) {
                return Optional.empty();
            }
            List<Student> found = new ArrayList<>(students.searchByName(text));
            try {
                Student byDocument = students.findByDocument(text);
                if (found.stream().noneMatch(s -> s.getId() == byDocument.getId())) {
                    found.add(0, byDocument);
                }
            } catch (ErpException e) {
                // the text was not a document (or no student has it): the name search result is enough
            }
            if (found.isEmpty()) {
                printer.warning("No se encontraron estudiantes con \"" + text + "\". Intente de nuevo.");
                continue;
            }
            if (found.size() == 1) {
                Student only = found.get(0);
                printer.info("Estudiante seleccionado: " + only.getFullName() + " (" + only.getDocumentNumber() + ")");
                return Optional.of(only);
            }
            if (found.size() > MAX_SEARCH_RESULTS) {
                printer.info("Hay " + found.size() + " coincidencias; se muestran las primeras "
                        + MAX_SEARCH_RESULTS + ". Puede cancelar y refinar la búsqueda.");
                found = found.subList(0, MAX_SEARCH_RESULTS);
            }
            return choose("Coincidencias", found, List.of("Documento", "Nombre", "Estado"),
                    s -> List.of(s.getDocumentNumber(), s.getFullName(), s.getStatus().toString()), Student::getId);
        }
    }

    /** Chooses one of the guardians of the given student. */
    public Optional<Guardian> guardianOf(int studentId) {
        return choose("Acudientes del estudiante", students.listGuardians(studentId),
                List.of("Nombre", "Parentesco", "Teléfono", "Responsable pago"),
                g -> List.of(g.getFullName(), g.getRelationship(), g.getPrimaryPhone(),
                        ViewFormat.yesNo(g.isFinanciallyResponsible())),
                Guardian::getId);
    }

    // ------------------------------------------------------------------ academic catalog

    public Optional<Grade> grade() {
        return choose("Grados", subjects.listGrades(), List.of("Grado"), g -> List.of(g.getName()), Grade::getId);
    }

    /** Chooses a subject from the given list (for example the subjects of one grade). */
    public Optional<Subject> subjectFrom(List<Subject> list) {
        return choose("Materias", list, List.of("Código", "Materia", "Horas/sem"),
                s -> List.of(s.getCode(), s.getName(), String.valueOf(s.getWeeklyHours())), Subject::getId);
    }

    /** First a grade, then one of its subjects (the catalog has 30+ subjects, too long for one list). */
    public Optional<Subject> subjectOfAnyGrade() {
        Optional<Grade> grade = grade();
        if (grade.isEmpty()) {
            return Optional.empty();
        }
        return subjectFrom(subjects.listSubjectsByGrade(grade.get().getId()));
    }

    /**
     * Chooses one of the subjects the student is enrolled in (ACTIVE) during the given year.
     * A subject enrolled in several periods is shown once: the stream keeps the first enrollment of each subject.
     */
    public Optional<SubjectEnrollment> enrolledSubject(int studentId, int academicYear) {
        List<SubjectEnrollment> distinct = new ArrayList<>(enrollments.listSubjectEnrollments(studentId, academicYear)
                .stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .collect(Collectors.toMap(SubjectEnrollment::getSubjectId, e -> e, (first, second) -> first,
                        LinkedHashMap::new))
                .values());
        Map<Integer, String> subjectNames = names.subjects();
        return choose("Materias inscritas en " + academicYear, distinct, List.of("Materia"),
                e -> List.of(NameLookup.of(subjectNames, e.getSubjectId())), SubjectEnrollment::getSubjectId);
    }

    public Optional<Teacher> teacher() {
        return choose("Profesores", teachers.listTeachers(), List.of("Nombre", "Especialización", "Estado"),
                t -> List.of(t.getFullName(), ViewFormat.text(t.getSpecialization()), t.getStatus().toString()),
                Teacher::getId);
    }

    // ------------------------------------------------------------------ extracurricular

    public Optional<Club> club() {
        return choose("Clubes", clubs.listClubs(), List.of("Club", "Cupo", "Libres", "Activo"),
                c -> List.of(c.getName(), String.valueOf(c.getMaxCapacity()),
                        String.valueOf(clubs.availableSeats(c.getId())), ViewFormat.yesNo(c.isActive())),
                Club::getId);
    }

    public Optional<SchoolEvent> event() {
        return choose("Eventos", events.listEvents(), List.of("Evento", "Tipo", "Fecha", "Competitivo"),
                e -> List.of(e.getName(), e.getType().toString(), ViewFormat.date(e.getEventDate()),
                        ViewFormat.yesNo(e.isCompetitive())),
                SchoolEvent::getId);
    }
}
