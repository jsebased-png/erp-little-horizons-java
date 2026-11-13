package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dto.DelinquentStudent;
import com.littlehorizons.erp.dto.PaymentRequest;
import com.littlehorizons.erp.dto.PaymentResult;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.strategy.DiscountStrategy;
import com.littlehorizons.erp.strategy.DiscountStrategyFactory;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConceptCodes;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.TaxCalculator;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Business rules for payments, discounts, VAT, invoice records and the delinquency report.
 * Flow of registerPayment: validate -> choose discount (Strategy) -> calculate VAT -> build payment and invoice
 * -> save both in ONE transaction (PaymentDao.registerWithInvoice). The PDF file is created afterwards (Phase 5).
 * Delinquent = active student whose last monthly tuition (PENSION) is more than 30 days old.
 */
// SOLID: SRP – payment rules; the discount math lives in the strategy classes and the VAT math in TaxCalculator
// SOLID: DIP – every collaborator is received by constructor (DAO interfaces and TaxCalculator)
public class PaymentController {

    /** Days after the last tuition payment before a student is reported as delinquent ("more than 1 month"). */
    public static final int DELINQUENCY_DAYS = 30;

    private final PaymentDao paymentDao;
    private final PaymentConceptDao conceptDao;
    private final InvoiceDao invoiceDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final TaxCalculator taxCalculator;
    private final String schoolNit;
    private final AppLogger logger = AppLogger.getInstance();

    public PaymentController(PaymentDao paymentDao, PaymentConceptDao conceptDao, InvoiceDao invoiceDao,
                             StudentDao studentDao, GuardianDao guardianDao, EnrollmentDao enrollmentDao,
                             TaxCalculator taxCalculator, String schoolNit) {
        this.paymentDao = paymentDao;
        this.conceptDao = conceptDao;
        this.invoiceDao = invoiceDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.taxCalculator = taxCalculator;
        this.schoolNit = schoolNit;
    }

    // ------------------------------------------------------------------ register a payment

    public PaymentResult registerPayment(PaymentRequest request) {
        Validator.requireNotNull("Payment method", request.method());
        Student student = studentDao.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Student not found: id " + request.studentId()));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessRuleException("Only ACTIVE students can make payments.");
        }
        PaymentConcept concept = conceptDao.findById(request.conceptId())
                .orElseThrow(() -> new NotFoundException("Payment concept not found: id " + request.conceptId()));
        if (!concept.isActive()) {
            throw new BusinessRuleException("The concept \"" + concept.getName() + "\" is not active.");
        }

        int year = request.academicYear() != null ? request.academicYear() : LocalDate.now().getYear();
        AcademicPeriod period = resolvePeriod(concept, request.period());
        checkEnrollmentFeeNotPaidTwice(student.getId(), concept, year);
        Guardian payer = resolvePayer(student.getId(), request.guardianId());
        checkScholarship(request.scholarshipPercent());

        // --- Strategy: the factory chooses the discount, we only call apply()
        boolean discountable = ConceptCodes.TUITION_GROUP.contains(concept.getCode());
        int activeSiblings = guardianDao.countActiveStudents(payer.getId());
        DiscountStrategy discountStrategy = DiscountStrategyFactory.create(discountable, activeSiblings,
                request.scholarshipPercent());

        BigDecimal baseAmount = concept.getBaseAmount().setScale(2);
        BigDecimal subtotal = discountStrategy.apply(baseAmount);
        BigDecimal discount = baseAmount.subtract(subtotal);
        BigDecimal vatRate = taxCalculator.rateFor(concept.getCode());
        BigDecimal vatAmount = taxCalculator.vatOf(subtotal, vatRate);
        BigDecimal total = subtotal.add(vatAmount);
        if (total.signum() <= 0) {
            throw new BusinessRuleException("The total to pay is zero, so no payment is needed.");
        }

        LocalDate today = LocalDate.now();
        Payment payment = new Payment();
        payment.setStudentId(student.getId());
        payment.setGuardianId(payer.getId());
        payment.setConceptId(concept.getId());
        payment.setAmountPaid(total);
        payment.setPaymentDate(today);
        payment.setMethod(request.method());
        payment.setReceiptNumber(String.format("REC-%d-%06d", today.getYear(), paymentDao.nextSequence()));
        payment.setAcademicYear(year);
        payment.setPeriod(period);
        payment.setNotes(buildNotes(discountStrategy, discount, vatRate));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(String.format("FE-%06d", invoiceDao.nextSequence()));
        invoice.setIssueDate(today);
        invoice.setCufe(CufeGenerator.generate(invoice.getInvoiceNumber(), schoolNit, today));

        // One transaction: payment + invoice are saved together or not at all
        paymentDao.registerWithInvoice(payment, invoice);
        logger.info(getClass(), "Payment " + payment.getReceiptNumber() + " saved with invoice " + invoice.getInvoiceNumber());

        return new PaymentResult(payment, invoice, student, payer, concept, baseAmount, discount, subtotal,
                vatRate, vatAmount, total, discountStrategy.describe());
    }

    /** Recurring concepts need an explicit period; the others are saved as "Anual". */
    private AcademicPeriod resolvePeriod(PaymentConcept concept, AcademicPeriod requested) {
        if (requested == AcademicPeriod.FINAL) {
            throw new ValidationException("Final is only for grades. Use I, II, III, IV or Anual.");
        }
        if (requested != null) {
            return requested;
        }
        if (concept.isRecurring()) {
            throw new ValidationException("The period (I, II, III, IV) is required for recurring concepts.");
        }
        return AcademicPeriod.ANNUAL;
    }

    private void checkEnrollmentFeeNotPaidTwice(int studentId, PaymentConcept concept, int year) {
        if (!ConceptCodes.ENROLLMENT_FEE.equals(concept.getCode())) {
            return;
        }
        boolean alreadyPaid = paymentDao.findByStudent(studentId).stream()
                .anyMatch(p -> p.getConceptId() == concept.getId()
                        && p.getAcademicYear() != null && p.getAcademicYear() == year);
        if (alreadyPaid) {
            throw new BusinessRuleException("The enrollment fee of " + year + " is already paid.");
        }
    }

    /** The payer is always known: the one chosen by the view, or the financial guardian of the student. */
    private Guardian resolvePayer(int studentId, Integer guardianId) {
        if (guardianId != null) {
            return guardianDao.findByStudentId(studentId).stream()
                    .filter(g -> g.getId() == guardianId)
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("That guardian is not linked to the student."));
        }
        return guardianDao.findFinancialGuardian(studentId)
                .orElseThrow(() -> new BusinessRuleException("The student has no financial guardian. Link one first."));
    }

    private void checkScholarship(BigDecimal percent) {
        if (percent != null && (percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0)) {
            throw new ValidationException("Scholarship percentage must be between 0 and 100.");
        }
    }

    private String buildNotes(DiscountStrategy strategy, BigDecimal discount, BigDecimal vatRate) {
        String vatText = "VAT " + vatRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
        return discount.signum() > 0
                ? strategy.describe() + " (-" + discount.toPlainString() + " COP). " + vatText
                : vatText;
    }

    // ------------------------------------------------------------------ queries

    public List<PaymentConcept> listActiveConcepts() {
        return conceptDao.findActive();
    }

    public List<Payment> historyByStudent(int studentId) {
        return paymentDao.findByStudent(studentId);
    }

    public List<Payment> historyByGuardian(int guardianId) {
        guardianDao.findById(guardianId).orElseThrow(() -> new NotFoundException("Guardian not found: id " + guardianId));
        return paymentDao.findByGuardian(guardianId);
    }

    public Optional<Invoice> findInvoiceOfPayment(int paymentId) {
        return invoiceDao.findByPayment(paymentId);
    }

    // ------------------------------------------------------------------ delinquency report

    /**
     * Active students with an active enrollment in the year whose last PENSION payment (or enrollment date,
     * if they never paid) is more than 30 days before today. Sorted from the most overdue.
     */
    public List<DelinquentStudent> findDelinquentStudents(int academicYear, LocalDate today) {
        // Pipeline 1: last tuition payment date per student (keeps the latest date when there are several)
        Map<Integer, LocalDate> lastTuitionPayment = paymentDao.findByConceptCode(ConceptCodes.MONTHLY_TUITION).stream()
                .collect(Collectors.toMap(Payment::getStudentId, Payment::getPaymentDate,
                        BinaryOperator.maxBy(Comparator.naturalOrder())));

        Map<Integer, Student> activeStudents = studentDao.findAll().stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .collect(Collectors.toMap(Student::getId, s -> s));

        // Pipeline 2: active enrollments -> overdue check -> keep only the delinquent ones
        return enrollmentDao.findByYear(academicYear).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .filter(e -> activeStudents.containsKey(e.getStudentId()))
                .map(e -> {
                    LocalDate lastPayment = lastTuitionPayment.get(e.getStudentId());
                    LocalDate reference = lastPayment != null ? lastPayment : e.getEnrollmentDate();
                    long days = ChronoUnit.DAYS.between(reference, today);
                    return days > DELINQUENCY_DAYS
                            ? Optional.of(new DelinquentStudent(e.getStudentId(),
                                    activeStudents.get(e.getStudentId()).getFullName(), reference, days, lastPayment == null))
                            : Optional.<DelinquentStudent>empty();
                })
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingLong(DelinquentStudent::daysOverdue).reversed())
                .toList();
    }

    // ------------------------------------------------------------------ revenue summary

    /** Total collected between two dates (both included). reduce: folds all amounts into one sum starting at ZERO. */
    public BigDecimal totalCollected(LocalDate from, LocalDate to) {
        checkDateRange(from, to);
        return paymentDao.findAll().stream()
                .filter(p -> !p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to))
                .map(Payment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Amount collected per concept name between two dates, the most profitable concept first. */
    public Map<String, BigDecimal> collectedByConcept(LocalDate from, LocalDate to) {
        checkDateRange(from, to);
        Map<Integer, String> conceptNames = conceptDao.findAll().stream()
                .collect(Collectors.toMap(PaymentConcept::getId, PaymentConcept::getName));

        Map<String, BigDecimal> totals = paymentDao.findAll().stream()
                .filter(p -> !p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to))
                .collect(Collectors.groupingBy(
                        p -> conceptNames.getOrDefault(p.getConceptId(), "Concept " + p.getConceptId()),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmountPaid, BigDecimal::add)));

        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private void checkDateRange(LocalDate from, LocalDate to) {
        Validator.requireNotNull("Start date", from);
        Validator.requireNotNull("End date", to);
        if (to.isBefore(from)) {
            throw new ValidationException("The end date cannot be before the start date.");
        }
    }
}
