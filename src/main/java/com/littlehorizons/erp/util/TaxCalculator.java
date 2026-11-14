package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates VAT for a payment concept.
 * It exists so the VAT rates come from config.properties and the rule "education 0 %, everything else 19 %"
 * lives in one place instead of inside the payment or invoice code.
 */
// SOLID: SRP – only tax math; rates are injected so it is easy to change or test
public class TaxCalculator {

    private final BigDecimal educationRate;
    private final BigDecimal otherRate;

    public TaxCalculator(BigDecimal educationRate, BigDecimal otherRate) {
        this.educationRate = educationRate;
        this.otherRate = otherRate;
    }

    /** VAT rate as a decimal (0.19 = 19 %) for the given concept code. */
    public BigDecimal rateFor(String conceptCode) {
        return ConceptCodes.TUITION_GROUP.contains(conceptCode) ? educationRate : otherRate;
    }

    /** VAT amount over a subtotal, rounded to cents. */
    public BigDecimal vatOf(BigDecimal subtotal, BigDecimal rate) {
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /** Subtotal contained in a VAT-inclusive total (used to rebuild an invoice from a saved payment). */
    public BigDecimal subtotalOf(BigDecimal total, BigDecimal rate) {
        return total.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
    }
}
