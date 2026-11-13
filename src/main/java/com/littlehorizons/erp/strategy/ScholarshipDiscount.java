package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;

/** Scholarship: the percentage is decided by the school staff when the payment is registered. */
public class ScholarshipDiscount extends PercentageDiscount {

    public ScholarshipDiscount(BigDecimal percent) {
        super(percent);
    }

    @Override
    public String describe() {
        return "Scholarship (" + getPercent().stripTrailingZeros().toPlainString() + "%)";
    }
}
