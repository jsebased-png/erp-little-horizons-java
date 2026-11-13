package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dto.PaymentResult;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.DatabaseException;
import com.littlehorizons.erp.exception.ErpException;
import com.littlehorizons.erp.exception.NotFoundException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.InvoiceGenerator;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.TaxCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Creates the invoice file (UTF-8 .txt, human-readable) and records its name and path in the database.
 * The payment and its invoice row are already saved (PaymentController, one transaction); this class runs AFTER that.
 * If the file cannot be written the payment is NOT undone: the failure is logged and the invoice can be regenerated later.
 * File names are unique because they come from the invoice number: FE-000123 -> factura_000123.txt.
 */
// SOLID: SRP – invoice file lifecycle only (name, location, record in DB); the content is in the InvoiceGenerator
// SOLID: DIP – receives the generator interface, the DAOs and the target folder by constructor
public class InvoiceController {

    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final PaymentConceptDao conceptDao;
    private final InvoiceGenerator generator;
    private final TaxCalculator taxCalculator;
    private final Path invoicesDir;
    private final AppLogger logger = AppLogger.getInstance();

    public InvoiceController(InvoiceDao invoiceDao, PaymentDao paymentDao, StudentDao studentDao,
                             GuardianDao guardianDao, PaymentConceptDao conceptDao, InvoiceGenerator generator,
                             TaxCalculator taxCalculator, Path invoicesDir) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.conceptDao = conceptDao;
        this.generator = generator;
        this.taxCalculator = taxCalculator;
        this.invoicesDir = invoicesDir;
    }

    /**
     * Called right after a payment is confirmed. Never throws for a file problem: it returns empty instead,
     * so the user still sees the receipt and a warning (the money was already recorded).
     */
    public Optional<Path> issuePdf(PaymentResult result) {
        try {
            return Optional.of(writePdf(result));
        } catch (ErpException e) {
            logger.error(getClass(), "Payment saved but the invoice file of "
                    + result.invoice().getInvoiceNumber() + " could not be created", e);
            return Optional.empty();
        }
    }

    /**
     * Builds the invoice file again from a saved payment (for example when the first attempt failed or the file was deleted).
     * The totals always match the payment: the subtotal and VAT are recalculated from the amount paid.
     * Limitation: the discount line is estimated as (current concept price - subtotal), and its text is generic.
     */
    public Path regeneratePdf(int paymentId) {
        Payment payment = paymentDao.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found: id " + paymentId));
        Invoice invoice = invoiceDao.findByPayment(paymentId)
                .orElseThrow(() -> new NotFoundException("The payment has no invoice record."));
        Student student = studentDao.findById(payment.getStudentId())
                .orElseThrow(() -> new NotFoundException("Student not found: id " + payment.getStudentId()));
        PaymentConcept concept = conceptDao.findById(payment.getConceptId())
                .orElseThrow(() -> new NotFoundException("Payment concept not found: id " + payment.getConceptId()));
        Guardian guardian = payment.getGuardianId() != null
                ? guardianDao.findById(payment.getGuardianId())
                        .orElseThrow(() -> new NotFoundException("Guardian not found: id " + payment.getGuardianId()))
                : guardianDao.findFinancialGuardian(student.getId())
                        .orElseThrow(() -> new BusinessRuleException("The student has no financial guardian."));

        BigDecimal rate = taxCalculator.rateFor(concept.getCode());
        BigDecimal total = payment.getAmountPaid().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = taxCalculator.subtotalOf(total, rate);
        BigDecimal vat = total.subtract(subtotal);
        BigDecimal discount = concept.getBaseAmount().subtract(subtotal).max(BigDecimal.ZERO).setScale(2);
        BigDecimal base = subtotal.add(discount); // keeps base - discount = subtotal even if the price changed later

        PaymentResult rebuilt = new PaymentResult(payment, invoice, student, guardian, concept, base, discount,
                subtotal, rate, vat, total, "Descuento aplicado");
        Path path = writePdf(rebuilt);
        logger.info(getClass(), "Invoice TXT regenerated: " + path);
        return path;
    }

    /** The saved invoice file, only if it still exists on disk. */
    public Optional<Path> findExistingPdf(Invoice invoice) {
        if (invoice.getPdfPath() == null || invoice.getPdfPath().isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(invoice.getPdfPath());
        return Files.exists(path) ? Optional.of(path) : Optional.empty();
    }

    // ------------------------------------------------------------------ internals

    /** Writes the file, then records name and path. A failure of the second step does not hide the created file. */
    private Path writePdf(PaymentResult result) {
        Invoice invoice = result.invoice();
        String fileName = fileNameOf(invoice);
        Path target = invoicesDir.resolve(fileName).toAbsolutePath().normalize();

        generator.generate(result, target); // may throw FileOperationException

        invoice.setPdfFileName(fileName);
        invoice.setPdfPath(target.toString());
        try {
            invoiceDao.updateFileInfo(invoice.getId(), fileName, target.toString());
        } catch (DatabaseException e) {
            logger.warning(getClass(), "Invoice created at " + target + " but its path was not saved in the database");
        }
        logger.info(getClass(), "Invoice TXT created: " + target);
        return target;
    }

    /** "FE-000123" -> "factura_000123.txt". Falls back to the invoice id if the number has no digits. */
    static String fileNameOf(Invoice invoice) {
        String digits = invoice.getInvoiceNumber() == null ? "" : invoice.getInvoiceNumber().replaceAll("\\D", "");
        if (digits.isEmpty()) {
            digits = String.format("%06d", invoice.getId());
        }
        return "factura_" + digits + ".txt";
    }
}
