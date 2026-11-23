package com.littlehorizons.erp.view;

import com.littlehorizons.erp.config.AppContext;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;

/**
 * Main menu (the 8 options of the assignment). It is the composition root of the view layer: it creates every
 * view once, gives each one only the controllers it needs, and sends the user to the chosen module.
 * It never talks to a DAO or to the database.
 */
// SOLID: DIP – views receive controllers through their constructors
// SOLID: SRP – builds the views and shows the main menu, nothing else
public class MainView {

    private final MenuEntries entries;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final AppContext context;

    /** The six entries of the main menu that open a module. */
    private record MenuEntries(StudentView students, AcademicView academic, TeacherView teachers,
                               ExtracurricularView extracurricular, FinanceView finance, ResourceView resources,
                               ReportView reports) {
    }

    public MainView(AppContext context, ConsoleInput input, ConsolePrinter printer) {
        this.context = context;
        this.input = input;
        this.printer = printer;
        this.entries = buildViews();
    }

    private MenuEntries buildViews() {
        PagedTable paged = new PagedTable(printer, input);
        NameLookup names = new NameLookup(context.students(), context.subjects(), context.teachers(),
                context.resources(), context.clubs(), context.events(), context.paymentConcepts());
        Pickers pickers = new Pickers(context.students(), context.subjects(), context.teachers(), context.clubs(),
                context.events(), context.enrollments(), names, input, printer);

        StudentView students = new StudentView(context.students(), pickers, input, printer, paged);
        EnrollmentView enrollmentView = new EnrollmentView(context.enrollments(), context.subjects(), names, pickers,
                input, printer, paged);
        GradeView gradeView = new GradeView(context.grades(), names, pickers, input, printer, paged);
        AttendanceView attendanceView = new AttendanceView(context.attendance(), names, pickers, input, printer, paged);
        SubjectView subjectView = new SubjectView(context.subjects(), names, pickers, input, printer, paged);
        SubjectPerformanceView subjectPerformanceView = new SubjectPerformanceView(context.performanceReports(), printer);
        AcademicView academic = new AcademicView(enrollmentView, gradeView, attendanceView, subjectView,
                subjectPerformanceView, input, printer);
        TeacherView teachers = new TeacherView(context.teachers(), names, pickers, input, printer, paged);
        ClubView clubView = new ClubView(context.clubs(), names, pickers, input, printer, paged);
        EventView eventView = new EventView(context.events(), names, pickers, input, printer, paged);
        ExtracurricularView extracurricular = new ExtracurricularView(clubView, eventView, input, printer);
        FinanceView finance = new FinanceView(context.payments(), context.invoices(), context.paymentConcepts(), names, pickers, input,
                printer, paged);
        ResourceView resources = new ResourceView(context.resources(), context.loans(), names, pickers, input,
                printer, paged);
        ReportView reports = new ReportView(context.performanceReports(), context.participationReports(),
                attendanceView, finance, resources, input, printer, paged);
        return new MenuEntries(students, academic, teachers, extracurricular, finance, resources, reports);
    }

    /** Shows the banner and the main menu until the user confirms the exit. */
    public void run() {
        printer.titleBox("COLEGIO INTERNACIONAL LITTLE HORIZONS S.A.S.");
        printer.println("                  Sistema ERP escolar - versión consola");
        markOverdueLoans();

        Menu mainMenu = new Menu("MENÚ PRINCIPAL", "Salir", input, printer)
                .addSubmenu("Gestión de Estudiantes", entries.students()::menu)
                .addSubmenu("Gestión Académica", entries.academic()::menu)
                .addSubmenu("Gestión de Profesores", entries.teachers()::menu)
                .addSubmenu("Actividades Extracurriculares", entries.extracurricular()::menu)
                .addSubmenu("Gestión Financiera", entries.finance()::menu)
                .addSubmenu("Gestión de Recursos", entries.resources()::menu)
                .addSubmenu("Reportes y Estadísticas", entries.reports()::menu)
                .exitAsLastNumber();

        do {
            mainMenu.run();
        } while (!input.confirm("¿Está seguro de que desea salir?"));
        printer.success("Gracias por usar el sistema. ¡Hasta pronto!");
    }

    /** "Vencido" is not set by the database by itself, so the loans are refreshed every time the program starts. */
    private void markOverdueLoans() {
        try {
            int changed = context.loans().refreshOverdueLoans();
            if (changed > 0) {
                printer.warning("Hay " + changed + " préstamo(s) que pasaron a estado Vencido. "
                        + "Revise Reportes > Préstamos vencidos.");
            }
        } catch (RuntimeException e) {
            // A failure here must not block the program; the error is logged and the menu still opens
            AppLogger.getInstance().error(MainView.class, "Could not refresh overdue loans", e);
        }
    }
}
