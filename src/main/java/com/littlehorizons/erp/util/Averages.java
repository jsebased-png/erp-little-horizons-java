package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Optional;

/**
 * Average of grades using BigDecimal.
 * It exists so averages are exact (no double rounding errors) and always rounded the same way: 1 decimal, HALF_UP.
 */
// SOLID: SRP – one calculation, used by every grade report
public final class Averages {

    private Averages() {
    }

    /** Returns empty when there are no values (a student without grades has no average, not a 0.0). */
    public static Optional<BigDecimal> of(Collection<BigDecimal> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        // reduce: folds the list into one sum, starting from ZERO
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP));
    }
}
