package com.littlehorizons.erp.config;

import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.GradeController;
import com.littlehorizons.erp.controller.InvoiceController;
import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ParticipationReportController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.PerformanceReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ResourceCategoryDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.impl.AcademicRecordDaoJdbc;
import com.littlehorizons.erp.dao.impl.AttendanceDaoJdbc;
import com.littlehorizons.erp.dao.impl.ClubDaoJdbc;
import com.littlehorizons.erp.dao.impl.ClubMembershipDaoJdbc;
import com.littlehorizons.erp.dao.impl.EnrollmentDaoJdbc;
import com.littlehorizons.erp.dao.impl.EventDaoJdbc;
import com.littlehorizons.erp.dao.impl.EventParticipantDaoJdbc;
import com.littlehorizons.erp.dao.impl.GradeDaoJdbc;
import com.littlehorizons.erp.dao.impl.GuardianDaoJdbc;
import com.littlehorizons.erp.dao.impl.InvoiceDaoJdbc;
import com.littlehorizons.erp.dao.impl.LoanDaoFactory;
import com.littlehorizons.erp.dao.impl.PaymentConceptDaoJdbc;
import com.littlehorizons.erp.dao.impl.PaymentDaoJdbc;
import com.littlehorizons.erp.dao.impl.ResourceCategoryDaoJdbc;
import com.littlehorizons.erp.dao.impl.ResourceDaoJdbc;
import com.littlehorizons.erp.dao.impl.StudentDaoJdbc;
import com.littlehorizons.erp.dao.impl.SubjectDaoJdbc;
import com.littlehorizons.erp.dao.impl.SubjectEnrollmentDaoJdbc;
import com.littlehorizons.erp.dao.impl.TeacherAssignmentDaoJdbc;
import com.littlehorizons.erp.dao.impl.TeacherDaoJdbc;
import com.littlehorizons.erp.exception.ConfigurationException;
import com.littlehorizons.erp.service.InvoiceGenerator;
import com.littlehorizons.erp.service.TextInvoiceGenerator;
import com.littlehorizons.erp.service.SchoolInfo;
import com.littlehorizons.erp.service.SubjectPerformanceTextWriter;
import com.littlehorizons.erp.util.ConfigLoader;
import com.littlehorizons.erp.util.TaxCalculator;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Composition root: the ONLY place where concrete classes are created and connected.
 * It builds the DAOs, gives them to the controllers through their constructors and exposes the controllers
 * to the views. It exists so no other class uses "new XxxDaoJdbc(...)" and every dependency is visible here.
 */
// SOLID: DIP – controllers know interfaces; this class is the single spot that knows the JDBC implementations
// SOLID: SRP – wiring only, no business logic
public class AppContext {

    private final StudentController studentController;
    private final SubjectController subjectController;
    private final EnrollmentController enrollmentController;
    private final GradeController gradeController;
    private final AttendanceController attendanceController;
    private final TeacherController teacherController;
    private final ClubController clubController;
    private final EventController eventController;
    private final PaymentController paymentController;
    private final InvoiceController invoiceController;
    private final PaymentConceptController paymentConceptController;
    private final ResourceController resourceController;
    private final LoanController loanController;
    private final PerformanceReportController performanceReportController;
    private final ParticipationReportController participationReportController;

    /** Reads config.properties, creates every DAO and controller. */
    public AppContext() {
        DatabaseConnection database = DatabaseConnection.getInstance();
        ConfigLoader config = new ConfigLoader();

        // ---- DAOs (interface type on the left, JDBC class on the right)
        StudentDao studentDao = new StudentDaoJdbc(database);
        GuardianDao guardianDao = new GuardianDaoJdbc(database);
        GradeDao gradeDao = new GradeDaoJdbc(database);
        EnrollmentDao enrollmentDao = new EnrollmentDaoJdbc(database);
        SubjectDao subjectDao = new SubjectDaoJdbc(database);
        SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDaoJdbc(database);
        TeacherDao teacherDao = new TeacherDaoJdbc(database);
        TeacherAssignmentDao assignmentDao = new TeacherAssignmentDaoJdbc(database);
        AcademicRecordDao recordDao = new AcademicRecordDaoJdbc(database);
        AttendanceDao attendanceDao = new AttendanceDaoJdbc(database);
        ClubDao clubDao = new ClubDaoJdbc(database);
        ClubMembershipDao membershipDao = new ClubMembershipDaoJdbc(database);
        EventDao eventDao = new EventDaoJdbc(database);
        EventParticipantDao participantDao = new EventParticipantDaoJdbc(database);
        PaymentConceptDao conceptDao = new PaymentConceptDaoJdbc(database);
        PaymentDao paymentDao = new PaymentDaoJdbc(database);
        InvoiceDao invoiceDao = new InvoiceDaoJdbc(database);
        ResourceCategoryDao categoryDao = new ResourceCategoryDaoJdbc(database);
        ResourceDao resourceDao = new ResourceDaoJdbc(database);
        LoanDaoFactory loanDaoFactory = new LoanDaoFactory(database);

        // ---- Taxes from config.properties
        TaxCalculator taxCalculator = new TaxCalculator(
                readRate(config, "tax.vat.education"), readRate(config, "tax.vat.other"));

        // ---- Controllers
        studentController = new StudentController(studentDao, guardianDao, enrollmentDao);
        subjectController = new SubjectController(subjectDao, gradeDao);
        enrollmentController = new EnrollmentController(enrollmentDao, subjectEnrollmentDao, studentDao, subjectDao);
        gradeController = new GradeController(recordDao, subjectEnrollmentDao, assignmentDao, subjectDao, studentDao);
        attendanceController = new AttendanceController(attendanceDao, subjectEnrollmentDao, studentDao);
        teacherController = new TeacherController(teacherDao, assignmentDao, subjectDao);
        clubController = new ClubController(clubDao, membershipDao, studentDao, teacherDao);
        eventController = new EventController(eventDao, participantDao, studentDao, teacherDao);
        paymentController = new PaymentController(paymentDao, conceptDao, invoiceDao, studentDao, guardianDao,
                enrollmentDao, taxCalculator, config.get("school.nit"));
        // Invoice TXT: human-readable UTF-8 file (open with any text editor). PDFBox classes remain in the project if needed.
        InvoiceGenerator invoiceGenerator = new TextInvoiceGenerator(SchoolInfo.fromConfig(config));
        Path invoicesDir = Path.of(config.get("invoices.dir", "invoices"));
        invoiceController = new InvoiceController(invoiceDao, paymentDao, studentDao, guardianDao, conceptDao,
                invoiceGenerator, taxCalculator, invoicesDir);
        paymentConceptController = new PaymentConceptController(conceptDao);
        resourceController = new ResourceController(resourceDao, categoryDao);
        // method reference: LoanDaoFactory::create becomes the Function<BorrowerType, LoanDao> the controller needs
        loanController = new LoanController(loanDaoFactory::create, resourceDao, studentDao, teacherDao);
        // report .txt files go to reports.dir (default: reports/), the same way invoices go to invoices.dir
        SubjectPerformanceTextWriter reportWriter = new SubjectPerformanceTextWriter(
                Path.of(config.get("reports.dir", "reports")));
        performanceReportController = new PerformanceReportController(recordDao, attendanceDao, studentDao, subjectDao,
                reportWriter);
        participationReportController = new ParticipationReportController(clubDao, membershipDao, eventDao,
                participantDao, studentDao);
    }

    private BigDecimal readRate(ConfigLoader config, String key) {
        try {
            return new BigDecimal(config.get(key));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Property " + key + " must be a decimal number such as 0.19", e);
        }
    }

    public StudentController students() { return studentController; }
    public SubjectController subjects() { return subjectController; }
    public EnrollmentController enrollments() { return enrollmentController; }
    public GradeController grades() { return gradeController; }
    public AttendanceController attendance() { return attendanceController; }
    public TeacherController teachers() { return teacherController; }
    public ClubController clubs() { return clubController; }
    public EventController events() { return eventController; }
    public PaymentController payments() { return paymentController; }
    public InvoiceController invoices() { return invoiceController; }
    public PaymentConceptController paymentConcepts() { return paymentConceptController; }
    public ResourceController resources() { return resourceController; }
    public LoanController loans() { return loanController; }
    public PerformanceReportController performanceReports() { return performanceReportController; }
    public ParticipationReportController participationReports() { return participationReportController; }
}
