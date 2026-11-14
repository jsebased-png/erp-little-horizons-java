package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens of the subject catalog (part of "2. Gestión Académica"): list, create, edit and activate/deactivate.
 */
// SOLID: SRP – subject catalog screens only
public class SubjectView {

    private final SubjectController subjects;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public SubjectView(SubjectController subjects, NameLookup names, Pickers pickers, ConsoleInput input,
                       ConsolePrinter printer, PagedTable pagedTable) {
        this.subjects = subjects;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("CATÁLOGO DE MATERIAS", "Volver", input, printer)
                .add("Listar todas las materias", this::listAll)
                .add("Listar materias de un grado", this::listByGrade)
                .add("Crear materia", this::create)
                .add("Editar materia", this::edit)
                .add("Activar / desactivar materia", this::toggleActive)
                .run();
    }

    private void listAll() {
        showTable(subjects.listSubjects());
    }

    private void listByGrade() {
        pickers.grade().ifPresent(g -> showTable(subjects.listSubjectsByGrade(g.getId())));
    }

    private void showTable(List<Subject> list) {
        Map<Integer, String> gradeNames = names.grades();
        List<List<String>> rows = list.stream()
                .map(s -> List.of(String.valueOf(s.getId()), s.getCode(), s.getName(),
                        NameLookup.of(gradeNames, s.getGradeId()), String.valueOf(s.getWeeklyHours()),
                        ViewFormat.yesNo(s.isActive())))
                .toList();
        pagedTable.show(List.of("ID", "Código", "Materia", "Grado", "Horas/sem", "Activa"), rows);
    }

    private void create() {
        Optional<Grade> grade = pickers.grade();
        if (grade.isEmpty()) {
            return;
        }
        Subject subject = new Subject();
        subject.setGradeId(grade.get().getId());
        subject.setCode(input.readText("Código de la materia (ej. MAT-05)"));
        subject.setName(input.readText("Nombre de la materia"));
        subject.setDescription(input.readOptionalText("Descripción"));
        subject.setWeeklyHours(input.readInt("Horas semanales", 1, 40));
        Subject saved = subjects.createSubject(subject);
        printer.success("Materia creada con ID " + saved.getId() + ".");
    }

    private void edit() {
        Optional<Subject> picked = pickers.subjectOfAnyGrade();
        if (picked.isEmpty()) {
            return;
        }
        Subject subject = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        subject.setCode(input.readTextOrKeep("Código", subject.getCode()));
        subject.setName(input.readTextOrKeep("Nombre", subject.getName()));
        subject.setDescription(input.readTextOrKeep("Descripción", subject.getDescription()));
        subject.setWeeklyHours(input.readIntOrDefault("Horas semanales", 1, 40, subject.getWeeklyHours()));
        subjects.updateSubject(subject);
        printer.success("Materia actualizada.");
    }

    private void toggleActive() {
        Optional<Subject> picked = pickers.subjectOfAnyGrade();
        if (picked.isEmpty()) {
            return;
        }
        Subject subject = picked.get();
        boolean newState = !subject.isActive();
        if (input.confirm((newState ? "¿Activar " : "¿Desactivar ") + subject.getName() + "?")) {
            subjects.setActive(subject.getId(), newState);
            printer.success("Materia " + (newState ? "activada." : "desactivada."));
        }
    }
}
