package com.littlehorizons.erp.view;

import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;

/**
 * Menu "2. Gestión Académica": groups the four academic areas. It has no logic, it only sends the user to the
 * view that handles each area.
 */
// SOLID: SRP – navigation only; OCP – a new academic area is one more add(...) line
public class AcademicView {

    private final EnrollmentView enrollmentView;
    private final GradeView gradeView;
    private final AttendanceView attendanceView;
    private final SubjectView subjectView;
    private final ConsoleInput input;
    private final ConsolePrinter printer;

    public AcademicView(EnrollmentView enrollmentView, GradeView gradeView, AttendanceView attendanceView,
                        SubjectView subjectView, ConsoleInput input, ConsolePrinter printer) {
        this.enrollmentView = enrollmentView;
        this.gradeView = gradeView;
        this.attendanceView = attendanceView;
        this.subjectView = subjectView;
        this.input = input;
        this.printer = printer;
    }

    public void menu() {
        new Menu("GESTIÓN ACADÉMICA", "Volver al menú principal", input, printer)
                .addSubmenu("Matrículas e inscripción en materias", enrollmentView::menu)
                .addSubmenu("Notas y promedios", gradeView::menu)
                .addSubmenu("Asistencia", attendanceView::menu)
                .addSubmenu("Catálogo de materias", subjectView::menu)
                .run();
    }
}
