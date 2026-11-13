package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dto.PaymentResult;
import com.littlehorizons.erp.exception.FileOperationException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Draws the simulated DIAN electronic invoice on one A4 page with PDFBox.
 * Page order: school header, buyer and student, item table, totals, payment data, CUFE, QR code and footer.
 * PDFBox coordinates start at the BOTTOM-left corner of the page, so "y" decreases as we move down.
 * Only the 14 standard PDF fonts are used (no font files to ship); text is cleaned to Latin-1 so accents work.
 */
// SOLID: SRP – this class only knows how to draw an invoice; amounts, numbers and CUFE arrive already calculated
public class PdfBoxInvoiceGenerator implements InvoiceGenerator {

    // PDFBox 2.x prints WARNING lines (font cache, fallback fonts) that would break the console menus.
    // This must be declared BEFORE the fonts below, because loading PDType1Font is what prints them.
    // The loggers are stored in static fields because java.util.logging keeps only weak references.
    private static final Logger PDFBOX_LOGGER = quiet("org.apache.pdfbox");
    private static final Logger FONTBOX_LOGGER = quiet("org.apache.fontbox");

    private static final float MARGIN = 40f;
    private static final float PAGE_RIGHT = PDRectangle.A4.getWidth() - MARGIN;
    private static final float CONTENT_WIDTH = PAGE_RIGHT - MARGIN;
    private static final float LINE_HEIGHT = 12f;

    private static final PDFont REGULAR = PDType1Font.HELVETICA;
    private static final PDFont BOLD = PDType1Font.HELVETICA_BOLD;
    private static final PDFont MONO = PDType1Font.COURIER;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int QR_PIXELS = 300;
    private static final float QR_POINTS = 110f;
    private static final int CUFE_LINE_LENGTH = 48;

    private final SchoolInfo school;
    private final QrCodeGenerator qrGenerator;

    // SOLID: DIP – school data and the QR generator are injected
    public PdfBoxInvoiceGenerator(SchoolInfo school, QrCodeGenerator qrGenerator) {
        this.school = school;
        this.qrGenerator = qrGenerator;
    }

    @Override
    public void generate(PaymentResult result, Path target) {
        // try-with-resources closes the document and the stream even when drawing fails
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream out = new PDPageContentStream(document, page)) {
                float y = drawHeader(out, result);
                y = drawParties(out, result, y - 8);
                y = drawItems(out, result, y - 22);
                y = drawTotalsAndPayment(out, result, y - 24);
                drawCufeAndQr(out, document, result, y - 24);
                drawFooter(out);
            }

            Files.createDirectories(target.toAbsolutePath().getParent());
            try (OutputStream file = Files.newOutputStream(target)) {
                document.save(file);
            }
        } catch (IOException e) {
            throw new FileOperationException("Could not write the invoice PDF: " + target, e);
        }
    }

    // ------------------------------------------------------------------ sections

    /** School data on the left, invoice title / number / date on the right, then the DIAN resolution. Returns next y. */
    private float drawHeader(PDPageContentStream out, PaymentResult r) throws IOException {
        text(out, BOLD, 15, MARGIN, 800, school.name());
        text(out, REGULAR, 9, MARGIN, 785, "NIT: " + school.nit());
        text(out, REGULAR, 9, MARGIN, 773, school.address());
        text(out, REGULAR, 9, MARGIN, 761, "Tel: " + school.phone() + "   |   " + school.email());

        textRight(out, BOLD, 11, PAGE_RIGHT, 800, "FACTURA ELECTRÓNICA DE VENTA");
        textRight(out, BOLD, 14, PAGE_RIGHT, 782, "No. " + r.invoice().getInvoiceNumber());
        textRight(out, REGULAR, 9, PAGE_RIGHT, 768, "Fecha de emisión: " + r.invoice().getIssueDate().format(DATE_FORMAT));

        line(out, 746);
        float y = 734;
        for (String text : wrap("Resolución de habilitación: " + school.dianResolution(), REGULAR, 8, CONTENT_WIDTH)) {
            text(out, REGULAR, 8, MARGIN, y, text);
            y -= 10;
        }
        return y;
    }

    /** Two columns: the guardian who pays (left) and the student (right). Returns the y below the taller column. */
    private float drawParties(PDPageContentStream out, PaymentResult r, float y) throws IOException {
        Guardian g = r.guardian();
        Student s = r.student();

        List<String> buyer = new ArrayList<>();
        buyer.add(g.getFullName());
        buyer.add(g.getDocumentType() + " " + g.getDocumentNumber());
        buyer.add("Tel: " + g.getPrimaryPhone());
        if (g.getEmail() != null) {
            buyer.add("Correo: " + g.getEmail());
        }
        if (g.getAddress() != null) {
            buyer.add("Dirección: " + g.getAddress());
        }

        List<String> student = new ArrayList<>();
        student.add(s.getFullName());
        student.add(s.getDocumentType() + " " + s.getDocumentNumber());
        student.add("Concepto: " + r.concept().getName());

        float rightColumnX = MARGIN + CONTENT_WIDTH / 2 + 10;
        float columnWidth = CONTENT_WIDTH / 2 - 10;
        text(out, BOLD, 9, MARGIN, y, "ADQUIRENTE (ACUDIENTE RESPONSABLE)");
        text(out, BOLD, 9, rightColumnX, y, "ESTUDIANTE");
        float leftEnd = drawLines(out, buyer, MARGIN, y - 13, columnWidth);
        float rightEnd = drawLines(out, student, rightColumnX, y - 13, columnWidth);
        return Math.min(leftEnd, rightEnd);
    }

    /** Item table: one line for the concept and, when there is a discount, one negative line. Returns y below it. */
    private float drawItems(PDPageContentStream out, PaymentResult r, float y) throws IOException {
        float descX = MARGIN + 4;
        float quantityRight = 360;
        float unitRight = 455;
        float subtotalRight = PAGE_RIGHT - 4;

        // grey header band
        out.setNonStrokingColor(0.88f);
        out.addRect(MARGIN, y - 4, CONTENT_WIDTH, 16);
        out.fill();
        out.setNonStrokingColor(0f);

        text(out, BOLD, 9, descX, y, "Descripción");
        textRight(out, BOLD, 9, quantityRight, y, "Cant.");
        textRight(out, BOLD, 9, unitRight, y, "Valor unitario");
        textRight(out, BOLD, 9, subtotalRight, y, "Subtotal");

        y -= 18;
        String period = r.payment().getPeriod() == null ? "" : " - Período " + r.payment().getPeriod();
        String year = r.payment().getAcademicYear() == null ? "" : " " + r.payment().getAcademicYear();
        text(out, REGULAR, 9, descX, y, r.concept().getName() + period + year);
        textRight(out, REGULAR, 9, quantityRight, y, "1");
        textRight(out, REGULAR, 9, unitRight, y, money(r.baseAmount()));
        textRight(out, REGULAR, 9, subtotalRight, y, money(r.baseAmount()));

        if (r.discount().signum() > 0) {
            y -= 14;
            text(out, REGULAR, 9, descX, y, "Descuento: " + r.discountDescription());
            textRight(out, REGULAR, 9, quantityRight, y, "1");
            textRight(out, REGULAR, 9, unitRight, y, "-" + money(r.discount()));
            textRight(out, REGULAR, 9, subtotalRight, y, "-" + money(r.discount()));
        }

        y -= 9;
        line(out, y);
        return y;
    }

    /** Payment data on the left, subtotal / VAT / total on the right. Returns the y below both. */
    private float drawTotalsAndPayment(PDPageContentStream out, PaymentResult r, float y) throws IOException {
        text(out, BOLD, 9, MARGIN, y, "DATOS DEL PAGO");
        text(out, REGULAR, 9, MARGIN, y - 13, "Método de pago: " + r.payment().getMethod());
        text(out, REGULAR, 9, MARGIN, y - 25, "Recibo de caja: " + r.payment().getReceiptNumber());
        text(out, REGULAR, 9, MARGIN, y - 37, "Fecha de pago: " + r.payment().getPaymentDate().format(DATE_FORMAT));

        float labelRight = 455;
        float valueRight = PAGE_RIGHT - 4;
        textRight(out, REGULAR, 10, labelRight, y, "Subtotal");
        textRight(out, REGULAR, 10, valueRight, y, money(r.subtotal()));
        textRight(out, REGULAR, 10, labelRight, y - 14, "IVA (" + percent(r.vatRate()) + ")");
        textRight(out, REGULAR, 10, valueRight, y - 14, money(r.vatAmount()));
        line(out, y - 22, 330, PAGE_RIGHT);
        textRight(out, BOLD, 12, labelRight, y - 38, "TOTAL");
        textRight(out, BOLD, 12, valueRight, y - 38, money(r.total()));
        return y - 44;
    }

    /** QR image on the left and the CUFE text on the right. */
    private void drawCufeAndQr(PDPageContentStream out, PDDocument document, PaymentResult r, float y)
            throws IOException {
        String cufe = r.invoice().getCufe() == null ? "" : r.invoice().getCufe();
        PDImageXObject qr = LosslessFactory.createFromImage(document, qrGenerator.generate(qrContent(r, cufe), QR_PIXELS));
        out.drawImage(qr, MARGIN, y - QR_POINTS, QR_POINTS, QR_POINTS);

        float textX = MARGIN + QR_POINTS + 18;
        float textY = y - 10;
        text(out, BOLD, 9, textX, textY, "CUFE (simulado)");
        textY -= 13;
        for (int start = 0; start < cufe.length(); start += CUFE_LINE_LENGTH) {
            text(out, MONO, 8, textX, textY, cufe.substring(start, Math.min(start + CUFE_LINE_LENGTH, cufe.length())));
            textY -= 11;
        }
        textY -= 6;
        for (String note : wrap("Código QR simulado: contiene número de factura, NIT, fecha, valores y CUFE. "
                + "El CUFE es el hash SHA-384 de número de factura + NIT + fecha.", REGULAR, 8, PAGE_RIGHT - textX)) {
            text(out, REGULAR, 8, textX, textY, note);
            textY -= 10;
        }
    }

    private void drawFooter(PDPageContentStream out) throws IOException {
        String note = "Representación gráfica simulada de factura electrónica. Proyecto académico, sin validez ante la DIAN.";
        float width = REGULAR.getStringWidth(clean(note)) / 1000f * 7;
        text(out, REGULAR, 7, (PDRectangle.A4.getWidth() - width) / 2, 40, note);
    }

    // ------------------------------------------------------------------ content of the QR

    private String qrContent(PaymentResult r, String cufe) {
        return "NumFac: " + r.invoice().getInvoiceNumber() + "\n"
                + "FecFac: " + r.invoice().getIssueDate() + "\n"
                + "NitFac: " + school.nit() + "\n"
                + "ValFac: " + r.subtotal().toPlainString() + "\n"
                + "ValIva: " + r.vatAmount().toPlainString() + "\n"
                + "ValTotal: " + r.total().toPlainString() + "\n"
                + "CUFE: " + cufe + "\n"
                + "SIMULADO - NO ES UN DOCUMENTO REAL DE LA DIAN";
    }

    // ------------------------------------------------------------------ small drawing helpers

    private void text(PDPageContentStream out, PDFont font, float size, float x, float y, String value)
            throws IOException {
        out.beginText();
        out.setFont(font, size);
        out.newLineAtOffset(x, y);
        out.showText(clean(value));
        out.endText();
    }

    /** Right-aligned text: the start x is the right edge minus the text width (PDFBox gives width in 1/1000 em). */
    private void textRight(PDPageContentStream out, PDFont font, float size, float rightX, float y, String value)
            throws IOException {
        float width = font.getStringWidth(clean(value)) / 1000f * size;
        text(out, font, size, rightX - width, y, value);
    }

    private void line(PDPageContentStream out, float y) throws IOException {
        line(out, y, MARGIN, PAGE_RIGHT);
    }

    private void line(PDPageContentStream out, float y, float fromX, float toX) throws IOException {
        out.setLineWidth(0.6f);
        out.moveTo(fromX, y);
        out.lineTo(toX, y);
        out.stroke();
    }

    /** Draws each text wrapped to maxWidth and returns the y where the next block can start. */
    private float drawLines(PDPageContentStream out, List<String> lines, float x, float y, float maxWidth)
            throws IOException {
        for (String item : lines) {
            for (String part : wrap(item, REGULAR, 9, maxWidth)) {
                text(out, REGULAR, 9, x, y, part);
                y -= LINE_HEIGHT;
            }
        }
        return y;
    }

    /** Splits a text in lines that fit maxWidth points. A single word longer than maxWidth is not cut. */
    private List<String> wrap(String value, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : clean(value).split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            boolean tooWide = font.getStringWidth(candidate) / 1000f * size > maxWidth;
            if (tooWide && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Standard PDF fonts only encode Latin-1 characters; PDFBox throws an exception for any other character
     * (for example an emoji typed in a name). We replace them by "?" so an invoice is never blocked by one symbol.
     */
    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder(value.length());
        // code points (not chars) so an emoji, which uses two chars, becomes a single "?"
        value.codePoints().forEach(codePoint -> {
            boolean printableLatin1 = (codePoint >= 0x20 && codePoint <= 0x7E) || (codePoint >= 0xA0 && codePoint <= 0xFF);
            clean.append(printableLatin1 ? (char) codePoint : (Character.isWhitespace(codePoint) ? ' ' : '?'));
        });
        return clean.toString();
    }

    private static Logger quiet(String name) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.SEVERE);
        return logger;
    }

    /** Colombian format with two decimals: $1.250.000,00 */
    private static String money(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("es-CO")));
        return "$" + format.format(amount);
    }

    /** 0.19 becomes "19%", 0.00 becomes "0%". */
    private static String percent(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }
}
