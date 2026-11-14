package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.util.List;
import java.util.Optional;

/**
 * Screens of "1. Gestión de Estudiantes": register, list, search, edit, withdraw and manage guardians.
 * It only asks for data, calls StudentController and prints the result; all the rules live in the controller.
 */
// SOLID: SRP – one view per module area; DIP – receives its controller and helpers through the constructor
public class StudentView {

    private final StudentController students;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public StudentView(StudentController students, Pickers pickers, ConsoleInput input, ConsolePrinter printer,
                       PagedTable pagedTable) {
        this.students = students;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("GESTIÓN DE ESTUDIANTES", "Volver al menú principal", input, printer)
                .add("Registrar estudiante (con su acudiente)", this::register)
                .add("Listar estudiantes", this::list)
                .add("Buscar estudiante y ver ficha", this::search)
                .add("Editar datos del estudiante", this::edit)
                .add("Retirar estudiante", this::withdraw)
                .addSubmenu("Gestionar acudientes y contactos de emergencia", this::manageGuardians)
                .run();
    }

    // ------------------------------------------------------------------ student operations

    private void register() {
        printer.section("Datos del estudiante");
        Student student = new Student();
        student.setDocumentType(input.readEnum("Tipo de documento", DocumentType.class));
        student.setDocumentNumber(input.readDocument(student.getDocumentType(), "Número de documento"));
        student.setFirstName(input.readPersonName("Primer nombre"));
        student.setMiddleName(input.readOptionalPersonName("Segundo nombre"));
        student.setFirstSurname(input.readPersonName("Primer apellido"));
        student.setSecondSurname(input.readOptionalPersonName("Segundo apellido"));
        student.setBirthDate(input.readDate("Fecha de nacimiento"));
        student.setGender(input.readEnum("Género", Gender.class));
        student.setAddress(input.readText("Dirección"));
        student.setPhone(input.readOptionalPhone("Teléfono"));
        student.setEmail(input.readOptionalEmail("Correo electrónico"));
        student.setBloodType(input.readOptionalBloodType("Tipo de sangre (ej. O+)"));
        student.setAllergies(input.readOptionalText("Alergias"));
        student.setMedicalConditions(input.readOptionalText("Condiciones médicas"));

        printer.section("Acudiente principal (contacto de emergencia)");
        printer.info("Si el documento del acudiente ya existe (por ejemplo un hermano), se reutiliza ese acudiente.");
        Guardian guardian = readGuardian();

        Student saved = students.registerStudent(student, guardian);
        printer.success("Estudiante registrado con ID " + saved.getId() + ": " + saved.getFullName());
    }

    private void list() {
        List<List<String>> rows = students.listStudents().stream()
                .map(s -> List.of(String.valueOf(s.getId()), s.getDocumentNumber(), s.getFullName(),
                        ViewFormat.date(s.getBirthDate()), s.getStatus().toString()))
                .toList();
        pagedTable.show(List.of("ID", "Documento", "Nombre", "Nacimiento", "Estado"), rows);
    }

    private void search() {
        pickers.student().ifPresent(this::showDetail);
    }

    private void edit() {
        Optional<Student> picked = pickers.student();
        if (picked.isEmpty()) {
            return;
        }
        Student student = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        student.setFirstName(input.readPersonNameOrKeep("Primer nombre", student.getFirstName()));
        student.setMiddleName(input.readPersonNameOrKeep("Segundo nombre", student.getMiddleName()));
        student.setFirstSurname(input.readPersonNameOrKeep("Primer apellido", student.getFirstSurname()));
        student.setSecondSurname(input.readPersonNameOrKeep("Segundo apellido", student.getSecondSurname()));
        student.setAddress(input.readTextOrKeep("Dirección", student.getAddress()));
        student.setPhone(input.readPhoneOrKeep("Teléfono", student.getPhone()));
        student.setEmail(input.readEmailOrKeep("Correo electrónico", student.getEmail()));
        student.setBloodType(input.readBloodTypeOrKeep("Tipo de sangre", student.getBloodType()));
        student.setAllergies(input.readTextOrKeep("Alergias", student.getAllergies()));
        student.setMedicalConditions(input.readTextOrKeep("Condiciones médicas", student.getMedicalConditions()));
        students.updateStudent(student);
        printer.success("Datos actualizados.");
    }

    private void withdraw() {
        Optional<Student> picked = pickers.student();
        if (picked.isEmpty()) {
            return;
        }
        Student student = picked.get();
        printer.warning("El retiro es lógico: el estudiante, sus notas y sus pagos se conservan.");
        if (input.confirm("¿Retirar a " + student.getFullName() + "?")) {
            students.withdrawStudent(student.getId());
            printer.success("Estudiante retirado.");
        } else {
            printer.info("Operación cancelada.");
        }
    }

    // ------------------------------------------------------------------ guardians

    private void manageGuardians() {
        Optional<Student> picked = pickers.student();
        if (picked.isEmpty()) {
            return;
        }
        Student student = picked.get();
        new Menu("ACUDIENTES DE " + student.getFirstName().toUpperCase() + " " + student.getFirstSurname().toUpperCase(),
                "Volver", input, printer)
                .add("Ver acudientes", () -> showGuardians(student.getId()))
                .add("Agregar acudiente", () -> addGuardian(student))
                .add("Editar datos de contacto de un acudiente", () -> editGuardian(student))
                .add("Quitar acudiente", () -> removeGuardian(student))
                .run();
    }

    private void addGuardian(Student student) {
        Guardian guardian = readGuardian();
        boolean primary = input.confirm("¿Es el contacto principal?");
        boolean emergency = input.confirm("¿Es contacto de emergencia?");
        students.addGuardian(student.getId(), guardian, primary, emergency);
        printer.success("Acudiente agregado.");
    }

    private void editGuardian(Student student) {
        Optional<Guardian> picked = pickers.guardianOf(student.getId());
        if (picked.isEmpty()) {
            return;
        }
        Guardian guardian = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        guardian.setPrimaryPhone(input.readTextOrKeep("Teléfono principal", guardian.getPrimaryPhone()));
        guardian.setSecondaryPhone(input.readTextOrKeep("Teléfono secundario", guardian.getSecondaryPhone()));
        guardian.setEmail(input.readEmailOrKeep("Correo electrónico", guardian.getEmail()));
        guardian.setAddress(input.readTextOrKeep("Dirección", guardian.getAddress()));
        guardian.setOccupation(input.readTextOrKeep("Ocupación", guardian.getOccupation()));
        students.updateGuardian(guardian);
        printer.success("Acudiente actualizado.");
    }

    private void removeGuardian(Student student) {
        Optional<Guardian> picked = pickers.guardianOf(student.getId());
        if (picked.isPresent() && input.confirm("¿Quitar a " + picked.get().getFullName() + " del estudiante?")) {
            students.removeGuardian(student.getId(), picked.get().getId());
            printer.success("Acudiente desvinculado.");
        }
    }

    private Guardian readGuardian() {
        Guardian guardian = new Guardian();
        // A guardian is an adult, so TI (identity card for minors) is not offered
        guardian.setDocumentType(input.readChoice("Tipo de documento",
                List.of(DocumentType.CC, DocumentType.CE, DocumentType.PASSPORT), DocumentType::toString));
        guardian.setDocumentNumber(input.readDocument(guardian.getDocumentType(), "Número de documento del acudiente"));
        guardian.setFirstName(input.readPersonName("Primer nombre del acudiente"));
        guardian.setMiddleName(input.readOptionalPersonName("Segundo nombre"));
        guardian.setFirstSurname(input.readPersonName("Primer apellido del acudiente"));
        guardian.setSecondSurname(input.readOptionalPersonName("Segundo apellido"));
        guardian.setRelationship(input.readRelationship("Parentesco (ej. Madre, Padre, Tío)"));
        guardian.setPrimaryPhone(input.readPhone("Teléfono principal"));
        guardian.setSecondaryPhone(input.readOptionalPhone("Teléfono secundario"));
        guardian.setEmail(input.readOptionalEmail("Correo electrónico"));
        guardian.setAddress(input.readOptionalText("Dirección"));
        guardian.setOccupation(input.readOptionalText("Ocupación"));
        guardian.setFinanciallyResponsible(input.confirm("¿Es el responsable financiero (paga la pensión)?"));
        return guardian;
    }

    // ------------------------------------------------------------------ detail screens

    private void showDetail(Student s) {
        printer.section("Ficha del estudiante");
        printer.field("ID", String.valueOf(s.getId()));
        printer.field("Nombre", s.getFullName());
        printer.field("Documento", s.getDocumentType() + " " + s.getDocumentNumber());
        printer.field("Fecha de nacimiento", ViewFormat.date(s.getBirthDate()));
        printer.field("Género", s.getGender().toString());
        printer.field("Dirección", s.getAddress());
        printer.field("Teléfono", s.getPhone());
        printer.field("Correo", s.getEmail());
        printer.field("Tipo de sangre", s.getBloodType());
        printer.field("Alergias", s.getAllergies());
        printer.field("Condiciones médicas", s.getMedicalConditions());
        printer.field("Fecha de ingreso", ViewFormat.date(s.getAdmissionDate()));
        printer.field("Estado", s.getStatus().toString());
        showGuardians(s.getId());
    }

    private void showGuardians(int studentId) {
        printer.section("Acudientes y contactos de emergencia");
        List<List<String>> rows = students.listGuardians(studentId).stream()
                .map(g -> List.of(g.getFullName(), g.getRelationship(), g.getPrimaryPhone(),
                        ViewFormat.yesNo(g.isPrimaryContact()), ViewFormat.yesNo(g.isEmergencyContact()),
                        ViewFormat.yesNo(g.isFinanciallyResponsible())))
                .toList();
        printer.table(List.of("Nombre", "Parentesco", "Teléfono", "Principal", "Emergencia", "Resp. pago"), rows);
    }
}
