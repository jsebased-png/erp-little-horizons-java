package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Default strategy: the student pays the full base amount. */
public class NoDiscount implements DiscountStrategy {

    @Override
    public BigDecimal apply(BigDecimal baseAmount) {
        return baseAmount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String describe() {
        return "No discount";
    }
}
