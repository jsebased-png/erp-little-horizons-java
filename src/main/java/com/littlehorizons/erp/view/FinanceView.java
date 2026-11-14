package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.InvoiceController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.dto.DelinquentStudent;
import com.littlehorizons.erp.dto.PaymentRequest;
import com.littlehorizons.erp.dto.PaymentResult;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.view.console.ConsoleInput;
import com.littlehorizons.erp.view.console.ConsolePrinter;
import com.littlehorizons.erp.view.console.Menu;
import com.littlehorizons.erp.view.console.PagedTable;
import com.littlehorizons.erp.view.console.ViewFormat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Screens of "5. Gestión Financiera": payments, history, invoices, delinquency, revenue and the concept catalog.
 * Discounts, taxes and the transaction are decided by PaymentController; this view collects the data and prints the receipt.
 */
// SOLID: SRP – finance screens only
public class FinanceView {

    /** Recurring concepts (tuition) are paid by period; "Anual" is also accepted. */
    private static final List<AcademicPeriod> PAYMENT_PERIODS = List.of(AcademicPeriod.I, AcademicPeriod.II,
            AcademicPeriod.III, AcademicPeriod.IV, AcademicPeriod.ANNUAL);

    private final PaymentController payments;
    private final InvoiceController invoices;
    private final PaymentConceptController concepts;
    private final NameLookup names;
    private final Pickers pickers;
    private final ConsoleInput input;
    private final ConsolePrinter printer;
    private final PagedTable pagedTable;

    public FinanceView(PaymentController payments, InvoiceController invoices, PaymentConceptController concepts, NameLookup names,
                       Pickers pickers, ConsoleInput input, ConsolePrinter printer, PagedTable pagedTable) {
        this.payments = payments;
        this.invoices = invoices;
        this.concepts = concepts;
        this.names = names;
        this.pickers = pickers;
        this.input = input;
        this.printer = printer;
        this.pagedTable = pagedTable;
    }

    public void menu() {
        new Menu("GESTIÓN FINANCIERA", "Volver al menú principal", input, printer)
                .add("Registrar pago (genera factura electrónica)", this::registerPayment)
                .add("Historial de pagos de un estudiante", this::historyByStudent)
                .add("Historial de pagos de un acudiente", this::historyByGuardian)
                .add("Consultar la factura de un pago", this::showInvoice)
                .add("Regenerar el archivo TXT de una factura", this::regenerateInvoicePdf)
                .add("Reporte de morosos (más de 1 mes)", this::showDelinquents)
                .add("Recaudo por rango de fechas", this::showRevenue)
                .addSubmenu("Conceptos de pago (catálogo)", this::conceptsMenu)
                .run();
    }

    // ------------------------------------------------------------------ register payment

    private void registerPayment() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Optional<PaymentConcept> concept = pickers.choose("Conceptos de pago activos", payments.listActiveConcepts(),
                List.of("Código", "Concepto", "Valor base", "Recurrente"),
                c -> List.of(c.getCode(), c.getName(), ViewFormat.money(c.getBaseAmount()),
                        ViewFormat.yesNo(c.isRecurring())),
                PaymentConcept::getId);
        if (concept.isEmpty()) {
            return;
        }
        PaymentMethod method = input.readEnum("Método de pago", PaymentMethod.class);

        AcademicPeriod period = null;
        if (concept.get().isRecurring()) {
            period = input.readChoice("Período que se paga", PAYMENT_PERIODS, AcademicPeriod::toString);
        }
        int year = input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());

        Integer guardianId = null;
        if (!input.confirm("¿Paga el acudiente responsable financiero?")) {
            Optional<Guardian> guardian = pickers.guardianOf(student.get().getId());
            if (guardian.isEmpty()) {
                return;
            }
            guardianId = guardian.get().getId();
        }

        BigDecimal scholarship = null;
        if (input.confirm("¿El estudiante tiene beca?")) {
            scholarship = input.readDecimal("Porcentaje de beca (1 a 100)", 1, 100);
        }

        printer.section("Resumen antes de confirmar");
        printer.field("Estudiante", student.get().getFullName());
        printer.field("Concepto", concept.get().getName());
        printer.field("Valor base", ViewFormat.money(concept.get().getBaseAmount()));
        printer.field("Método", method.toString());
        printer.info("El descuento por hermanos o beca y el IVA se calculan al confirmar.");
        if (!input.confirm("¿Confirmar el pago?")) {
            printer.info("Pago cancelado.");
            return;
        }

        PaymentResult result = payments.registerPayment(new PaymentRequest(student.get().getId(),
                concept.get().getId(), guardianId, method, period, year, scholarship));
        showReceipt(result);
        showPdfResult(invoices.issuePdf(result));
    }

    /** The payment is already saved at this point, so an invoice-file problem is a warning, not an error. */
    private void showPdfResult(Optional<Path> pdf) {
        if (pdf.isPresent()) {
            printer.success("Factura electrónica TXT generada.");
            printer.field("Ruta del archivo TXT", pdf.get().toString());
        } else {
            printer.warning("El pago quedó registrado, pero no se pudo crear el archivo TXT de la factura.");
            printer.info("Use «Regenerar el archivo TXT de una factura» o revise logs/error_log.txt.");
        }
    }

    private void showReceipt(PaymentResult r) {
        printer.success("Pago registrado correctamente.");
        printer.section("Recibo " + r.payment().getReceiptNumber());
        printer.field("Fecha", ViewFormat.date(r.payment().getPaymentDate()));
        printer.field("Estudiante", r.student().getFullName());
        printer.field("Acudiente", r.guardian().getFullName());
        printer.field("Concepto", r.concept().getName());
        printer.field("Período / año", r.payment().getPeriod() + " / " + r.payment().getAcademicYear());
        printer.field("Método", r.payment().getMethod().toString());
        printer.field("Valor base", ViewFormat.money(r.baseAmount()));
        printer.field("Descuento", ViewFormat.money(r.discount())
                + (r.discount().signum() > 0 ? "  (" + r.discountDescription() + ")" : ""));
        printer.field("Subtotal", ViewFormat.money(r.subtotal()));
        printer.field("IVA (" + ViewFormat.percent(r.vatRate().doubleValue()) + ")", ViewFormat.money(r.vatAmount()));
        printer.field("TOTAL PAGADO", ViewFormat.money(r.total()));
        printer.field("Factura electrónica", r.invoice().getInvoiceNumber());
        printer.field("CUFE (simulado)", shorten(r.invoice().getCufe()));
    }

    // ------------------------------------------------------------------ history and invoices

    private void historyByStudent() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        printer.section("Pagos de " + student.get().getFullName());
        showPayments(payments.historyByStudent(student.get().getId()));
    }

    private void historyByGuardian() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Optional<Guardian> guardian = pickers.guardianOf(student.get().getId());
        if (guardian.isEmpty()) {
            return;
        }
        printer.section("Pagos hechos por " + guardian.get().getFullName());
        showPayments(payments.historyByGuardian(guardian.get().getId()));
    }

    private void showPayments(List<Payment> list) {
        Map<Integer, String> conceptNames = names.concepts();
        Map<Integer, String> studentNames = names.students();
        List<List<String>> rows = list.stream()
                .map(p -> List.of(String.valueOf(p.getId()), p.getReceiptNumber(), ViewFormat.date(p.getPaymentDate()),
                        NameLookup.of(studentNames, p.getStudentId()), NameLookup.of(conceptNames, p.getConceptId()),
                        ViewFormat.text(p.getPeriod()), p.getMethod().toString(), ViewFormat.money(p.getAmountPaid())))
                .toList();
        pagedTable.show(List.of("ID", "Recibo", "Fecha", "Estudiante", "Concepto", "Período", "Método", "Monto"), rows);
        BigDecimal total = list.stream().map(Payment::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        printer.info("Total de los pagos listados: " + ViewFormat.money(total));
    }

    private void showInvoice() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> conceptNames = names.concepts();
        Optional<Payment> payment = pickers.choose("Pagos del estudiante", payments.historyByStudent(student.get().getId()),
                List.of("Recibo", "Fecha", "Concepto", "Monto"),
                p -> List.of(p.getReceiptNumber(), ViewFormat.date(p.getPaymentDate()),
                        NameLookup.of(conceptNames, p.getConceptId()), ViewFormat.money(p.getAmountPaid())),
                Payment::getId);
        if (payment.isEmpty()) {
            return;
        }
        Optional<Invoice> invoice = payments.findInvoiceOfPayment(payment.get().getId());
        if (invoice.isEmpty()) {
            printer.warning("Ese pago no tiene factura registrada.");
            return;
        }
        Invoice i = invoice.get();
        printer.section("Factura " + i.getInvoiceNumber());
        printer.field("Fecha de emisión", ViewFormat.date(i.getIssueDate()));
        printer.field("CUFE (simulado)", i.getCufe());
        Optional<Path> pdf = invoices.findExistingPdf(i);
        printer.field("Archivo TXT", ViewFormat.text(i.getPdfFileName()));
        printer.field("Ruta", ViewFormat.text(i.getPdfPath()));
        if (pdf.isEmpty()) {
            printer.warning("El archivo TXT no existe en disco. Use «Regenerar el archivo TXT de una factura».");
        }
    }

    private void regenerateInvoicePdf() {
        Optional<Student> student = pickers.student();
        if (student.isEmpty()) {
            return;
        }
        Map<Integer, String> conceptNames = names.concepts();
        Optional<Payment> payment = pickers.choose("Pagos del estudiante", payments.historyByStudent(student.get().getId()),
                List.of("Recibo", "Fecha", "Concepto", "Monto"),
                p -> List.of(p.getReceiptNumber(), ViewFormat.date(p.getPaymentDate()),
                        NameLookup.of(conceptNames, p.getConceptId()), ViewFormat.money(p.getAmountPaid())),
                Payment::getId);
        if (payment.isEmpty()) {
            return;
        }
        Path pdf = invoices.regeneratePdf(payment.get().getId());
        printer.success("Factura TXT generada de nuevo.");
        printer.field("Ruta del archivo TXT", pdf.toString());
    }

    // ------------------------------------------------------------------ reports (also opened from the reports menu)

    /** Public because the reports menu reuses this same screen. */
    public void showDelinquents() {
        int year = input.readIntOrDefault("Año académico", 2000, 2100, LocalDate.now().getYear());
        List<DelinquentStudent> delinquents = payments.findDelinquentStudents(year, LocalDate.now());
        List<List<String>> rows = delinquents.stream()
                .map(d -> List.of(d.studentName(), ViewFormat.date(d.referenceDate()),
                        d.neverPaid() ? "Nunca ha pagado pensión" : "Último pago de pensión",
                        String.valueOf(d.daysOverdue())))
                .toList();
        printer.section("Morosos: pensión sin pagar hace más de " + PaymentController.DELINQUENCY_DAYS + " días");
        pagedTable.show(List.of("Estudiante", "Fecha de referencia", "Referencia", "Días de mora"), rows);
    }

    /** Public because the reports menu reuses this same screen. */
    public void showRevenue() {
        LocalDate to = input.readDateOrDefault("Fecha final", LocalDate.now());
        LocalDate from = input.readDateOrDefault("Fecha inicial", to.withDayOfMonth(1));
        Map<String, BigDecimal> byConcept = payments.collectedByConcept(from, to);
        BigDecimal max = byConcept.values().stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ONE);
        List<List<String>> rows = byConcept.entrySet().stream()
                .map(e -> List.of(e.getKey(), ViewFormat.money(e.getValue()),
                        ViewFormat.bar(e.getValue().doubleValue() / max.doubleValue(), 20)))
                .toList();
        printer.section("Recaudo del " + ViewFormat.date(from) + " al " + ViewFormat.date(to));
        printer.table(List.of("Concepto", "Recaudado", ""), rows);
        printer.success("Total recaudado: " + ViewFormat.money(payments.totalCollected(from, to)));
    }

    // ------------------------------------------------------------------ concept catalog

    private void conceptsMenu() {
        new Menu("CONCEPTOS DE PAGO", "Volver", input, printer)
                .add("Listar conceptos", this::listConcepts)
                .add("Crear concepto", this::createConcept)
                .add("Editar concepto (valor base)", this::editConcept)
                .add("Activar / desactivar concepto", this::toggleConcept)
                .run();
    }

    private void listConcepts() {
        List<List<String>> rows = concepts.listConcepts().stream()
                .map(c -> List.of(String.valueOf(c.getId()), c.getCode(), c.getName(),
                        ViewFormat.money(c.getBaseAmount()), ViewFormat.yesNo(c.isRecurring()),
                        ViewFormat.yesNo(c.isActive())))
                .toList();
        pagedTable.show(List.of("ID", "Código", "Concepto", "Valor base", "Recurrente", "Activo"), rows);
    }

    private Optional<PaymentConcept> chooseConcept() {
        return pickers.choose("Conceptos de pago", concepts.listConcepts(),
                List.of("Código", "Concepto", "Valor base", "Activo"),
                c -> List.of(c.getCode(), c.getName(), ViewFormat.money(c.getBaseAmount()),
                        ViewFormat.yesNo(c.isActive())),
                PaymentConcept::getId);
    }

    private void createConcept() {
        PaymentConcept concept = new PaymentConcept();
        concept.setCode(input.readText("Código del concepto (ej. UNIF)"));
        concept.setName(input.readText("Nombre del concepto"));
        concept.setDescription(input.readOptionalText("Descripción"));
        concept.setBaseAmount(input.readDecimal("Valor base en COP", 0, 100_000_000));
        concept.setRecurring(input.confirm("¿Es un cobro recurrente (por período)?"));
        PaymentConcept saved = concepts.createConcept(concept);
        printer.success("Concepto creado con ID " + saved.getId() + ".");
    }

    private void editConcept() {
        Optional<PaymentConcept> picked = chooseConcept();
        if (picked.isEmpty()) {
            return;
        }
        PaymentConcept concept = picked.get();
        printer.info("Presione ENTER en cada dato para dejarlo igual.");
        concept.setName(input.readTextOrKeep("Nombre", concept.getName()));
        concept.setDescription(input.readTextOrKeep("Descripción", concept.getDescription()));
        String amount = input.readTextOrKeep("Valor base en COP", concept.getBaseAmount().toPlainString());
        try {
            concept.setBaseAmount(new BigDecimal(amount.replace(',', '.')));
        } catch (NumberFormatException e) {
            printer.warning("Valor no válido; se conserva el valor base actual.");
        }
        concepts.updateConcept(concept);
        printer.success("Concepto actualizado.");
    }

    private void toggleConcept() {
        Optional<PaymentConcept> picked = chooseConcept();
        if (picked.isEmpty()) {
            return;
        }
        boolean newState = !picked.get().isActive();
        if (input.confirm((newState ? "¿Activar " : "¿Desactivar ") + picked.get().getName() + "?")) {
            concepts.setActive(picked.get().getId(), newState);
            printer.success("Concepto " + (newState ? "activado." : "desactivado."));
        }
    }

    private String shorten(String text) {
        return text == null || text.length() <= 40 ? text : text.substring(0, 40) + "…";
    }
}
