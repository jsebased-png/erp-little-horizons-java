package com.littlehorizons.erp.dto;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;

import java.math.BigDecimal;

/**
 * Everything the invoice generator (Phase 5) and the view need after a payment is saved.
 * It carries the student, the paying guardian and the concept so the PDF code does not query the database again.
 *
 * @param baseAmount  concept value before discounts
 * @param discount    amount discounted (zero if none)
 * @param subtotal    base amount minus discount
 * @param vatRate     VAT rate as a decimal (0.19 = 19 %)
 * @param vatAmount   VAT in pesos
 * @param total       subtotal + VAT (this is what the payment stores)
 * @param discountDescription text such as "Siblings discount (10%)"
 */
public record PaymentResult(Payment payment, Invoice invoice, Student student, Guardian guardian,
                            PaymentConcept concept, BigDecimal baseAmount, BigDecimal discount,
                            BigDecimal subtotal, BigDecimal vatRate, BigDecimal vatAmount, BigDecimal total,
                            String discountDescription) {
}
