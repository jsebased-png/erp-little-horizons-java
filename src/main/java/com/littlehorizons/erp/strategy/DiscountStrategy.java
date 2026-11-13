package com.littlehorizons.erp.strategy;

import java.math.BigDecimal;

/**
 * Strategy contract: how a discount is applied to a base amount.
 * It exists so PaymentController can calculate any discount without if/else on the discount type.
 */
// Design pattern: Strategy – each discount rule is a separate, interchangeable class
// SOLID: OCP – a new discount (for example "staff children") is a new class; PaymentController does not change
public interface DiscountStrategy {

    /** Returns the amount to charge after applying the discount (scale 2). */
    BigDecimal apply(BigDecimal baseAmount);

    /** Short text for the receipt and the payment notes. */
    String describe();
}
