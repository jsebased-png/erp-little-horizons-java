package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Base for discounts expressed as a percentage.
 * It exists so SiblingsDiscount and ScholarshipDiscount share the calculation and differ only in label and rate.
 */
public abstract class PercentageDiscount implements DiscountStrategy {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal percent;

    protected PercentageDiscount(BigDecimal percent) {
        this.percent = percent;
    }

    @Override
    public BigDecimal apply(BigDecimal baseAmount) {
        BigDecimal discount = baseAmount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return baseAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal getPercent() {
        return percent;
    }
}
