package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;

/** Fixed 10 % discount when the guardian has two or more active students in the school. */
public class SiblingsDiscount extends PercentageDiscount {

    public static final BigDecimal PERCENT = new BigDecimal("10");

    public SiblingsDiscount() {
        super(PERCENT);
    }

    @Override
    public String describe() {
        return "Siblings discount (" + PERCENT.stripTrailingZeros().toPlainString() + "%)";
    }
}
