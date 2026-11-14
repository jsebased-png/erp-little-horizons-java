package com.littlehorizons.erp.util;

import java.util.Set;

/**
 * Codes of the payment concepts that have special rules (they exist in the seed data of conceptos_pago).
 * It exists so the tax rule and the discount rule use the same constants instead of repeating strings.
 */
public final class ConceptCodes {

    public static final String ENROLLMENT_FEE = "MAT";
    public static final String MONTHLY_TUITION = "PENSION";

    /** Formal education: VAT-excluded in Colombia, and the only concepts that accept discounts. */
    public static final Set<String> TUITION_GROUP = Set.of(ENROLLMENT_FEE, MONTHLY_TUITION);

    private ConceptCodes() {
    }
}
