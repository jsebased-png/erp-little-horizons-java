package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dto.PaymentResult;
import com.littlehorizons.erp.exception.FileOperationException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Writes a simulated DIAN electronic invoice as a plain UTF-8 .txt file.
 * Chosen so any evaluator can open the invoice without a PDF reader: notepad, IDE, cat, etc.
 * Layout mirrors the PDF version: school header, buyer, student, item line, totals, CUFE, QR payload and footer.
 */
// SOLID: SRP – only formats and writes the invoice text; amounts and CUFE arrive already calculated
// SOLID: OCP – implements InvoiceGenerator; AppContext can switch back to PdfBoxInvoiceGenerator without changing controllers
public class TextInvoiceGenerator implements InvoiceGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int WIDTH = 72;

    private final SchoolInfo school;

    public TextInvoiceGenerator(SchoolInfo school) {
        this.school = school;
    }

    @Override
    public void generate(PaymentResult result, Path targetFile) {
        try {
            Path parent = targetFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String content = buildContent(result);
            Files.writeString(targetFile, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileOperationException("Could not write invoice text file: " + targetFile, e);
        }
    }

    private String buildContent(PaymentResult result) {
        Invoice invoice = result.invoice();
        Payment payment = result.payment();
        Student student = result.student();
        Guardian guardian = result.guardian();
        PaymentConcept concept = result.concept();

        StringBuilder sb = new StringBuilder();
        line(sb, repeat('=', WIDTH));
        center(sb, "FACTURA ELECTRONICA DE VENTA (SIMULADA DIAN)");
        center(sb, school.name());
        line(sb, repeat('=', WIDTH));
        sb.append('\n');

        sb.append("NIT: ").append(school.nit()).append('\n');
        sb.append("Direccion: ").append(school.address()).append('\n');
        sb.append("Telefono: ").append(school.phone()).append('\n');
        sb.append("Correo: ").append(school.email()).append('\n');
        sb.append("Resolucion: ").append(school.dianResolution()).append('\n');
        sb.append('\n');

        line(sb, repeat('-', WIDTH));
        sb.append("Numero factura : ").append(nullSafe(invoice.getInvoiceNumber())).append('\n');
        sb.append("Fecha emision  : ").append(invoice.getIssueDate() != null
                ? invoice.getIssueDate().format(DATE_FMT) : "").append('\n');
        sb.append("Recibo pago    : ").append(nullSafe(payment.getReceiptNumber())).append('\n');
        sb.append("Metodo pago    : ").append(payment.getMethod() != null
                ? payment.getMethod().toString() : "").append('\n');
        line(sb, repeat('-', WIDTH));
        sb.append('\n');

        sb.append("ADQUIRIENTE (acudiente responsable financiero)\n");
        sb.append("  Nombre   : ").append(fullName(
                guardian.getFirstName(), guardian.getMiddleName(),
                guardian.getFirstSurname(), guardian.getSecondSurname())).append('\n');
        sb.append("  Documento: ").append(guardian.getDocumentType()).append(' ')
                .append(nullSafe(guardian.getDocumentNumber())).append('\n');
        sb.append("  Telefono : ").append(nullSafe(guardian.getPrimaryPhone())).append('\n');
        sb.append("  Correo   : ").append(nullSafe(guardian.getEmail())).append('\n');
        sb.append('\n');

        sb.append("ESTUDIANTE\n");
        sb.append("  Nombre   : ").append(fullName(
                student.getFirstName(), student.getMiddleName(),
                student.getFirstSurname(), student.getSecondSurname())).append('\n');
        sb.append("  Documento: ").append(student.getDocumentType()).append(' ')
                .append(nullSafe(student.getDocumentNumber())).append('\n');
        sb.append('\n');

        line(sb, repeat('-', WIDTH));
        sb.append(String.format("%-6s %-28s %8s %12s %12s%n",
                "Cant.", "Descripcion", "V. Unit.", "Subtotal", "IVA"));
        line(sb, repeat('-', WIDTH));

        String description = concept.getName();
        if (result.discountDescription() != null && !result.discountDescription().isBlank()) {
            description = description + " (" + result.discountDescription() + ")";
        }
        if (description.length() > 28) {
            description = description.substring(0, 25) + "...";
        }

        sb.append(String.format("%-6s %-28s %12s %12s %12s%n",
                "1",
                description,
                money(result.baseAmount()),
                money(result.subtotal()),
                money(result.vatAmount())));
        line(sb, repeat('-', WIDTH));

        sb.append(String.format("%47s %12s%n", "Subtotal:", money(result.subtotal())));
        sb.append(String.format("%47s %12s%n",
                "IVA (" + percent(result.vatRate()) + "):", money(result.vatAmount())));
        sb.append(String.format("%47s %12s%n", "TOTAL:", money(result.total())));
        if (result.discount() != null && result.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%47s %12s%n", "Descuento aplicado:", money(result.discount())));
        }
        sb.append('\n');

        line(sb, repeat('=', WIDTH));
        sb.append("CUFE (simulado):\n");
        sb.append(nullSafe(invoice.getCufe())).append('\n');
        sb.append('\n');
        sb.append("QR (contenido textual simulado):\n");
        sb.append("  NumFactura=").append(nullSafe(invoice.getInvoiceNumber())).append('\n');
        sb.append("  NIT=").append(school.nit()).append('\n');
        sb.append("  Fecha=").append(invoice.getIssueDate() != null
                ? invoice.getIssueDate().format(DATE_FMT) : "").append('\n');
        sb.append("  Total=").append(money(result.total())).append('\n');
        sb.append("  CUFE=").append(nullSafe(invoice.getCufe())).append('\n');
        line(sb, repeat('=', WIDTH));
        center(sb, "Documento generado por Little Horizons ERP (simulacion)");
        center(sb, "No tiene validez tributaria real ante la DIAN");
        line(sb, repeat('=', WIDTH));
        sb.append('\n');

        return sb.toString();
    }

    private static void line(StringBuilder sb, String s) {
        sb.append(s).append('\n');
    }

    private static void center(StringBuilder sb, String text) {
        if (text.length() >= WIDTH) {
            sb.append(text).append('\n');
            return;
        }
        int pad = (WIDTH - text.length()) / 2;
        sb.append(" ".repeat(pad)).append(text).append('\n');
    }

    private static String repeat(char c, int n) {
        return String.valueOf(c).repeat(Math.max(0, n));
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String fullName(String first, String middle, String last1, String last2) {
        StringBuilder n = new StringBuilder();
        if (first != null && !first.isBlank()) {
            n.append(first.trim());
        }
        if (middle != null && !middle.isBlank()) {
            if (n.length() > 0) {
                n.append(' ');
            }
            n.append(middle.trim());
        }
        if (last1 != null && !last1.isBlank()) {
            if (n.length() > 0) {
                n.append(' ');
            }
            n.append(last1.trim());
        }
        if (last2 != null && !last2.isBlank()) {
            if (n.length() > 0) {
                n.append(' ');
            }
            n.append(last2.trim());
        }
        return n.toString();
    }

    private static String money(BigDecimal value) {
        if (value == null) {
            return "0,00";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "$ " + df.format(value);
    }

    private static String percent(BigDecimal rate) {
        if (rate == null) {
            return "0%";
        }
        return rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
