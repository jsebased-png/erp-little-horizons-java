package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chooses the discount that applies to a payment.
 * It exists so the controller asks "which discount?" and receives a ready DiscountStrategy.
 * Discounts are NOT stacked: the student receives the best one (the one that charges less).
 */
// Design pattern: Factory – hides which concrete DiscountStrategy is created
public final class DiscountStrategyFactory {

    private static final BigDecimal REFERENCE_AMOUNT = new BigDecimal("100");

    private DiscountStrategyFactory() {
    }

    /**
     * @param discountable      false for concepts that never accept discounts (uniforms, transport...)
     * @param activeSiblings    active students of the paying guardian (2 or more means siblings)
     * @param scholarshipPercent scholarship percentage, may be null
     */
    public static DiscountStrategy create(boolean discountable, int activeSiblings, BigDecimal scholarshipPercent) {
        if (!discountable) {
            return new NoDiscount();
        }
        List<DiscountStrategy> candidates = new ArrayList<>();
        if (activeSiblings >= 2) {
            candidates.add(new SiblingsDiscount());
        }
        if (scholarshipPercent != null && scholarshipPercent.signum() > 0) {
            candidates.add(new ScholarshipDiscount(scholarshipPercent));
        }
        // Stream + lambda: compare candidates by what each one would charge over a reference amount of 100
        return candidates.stream()
                .min(Comparator.comparing((DiscountStrategy s) -> s.apply(REFERENCE_AMOUNT)))
                .orElseGet(NoDiscount::new);
    }
}
