package com.littlehorizons.erp.util;

import com.littlehorizons.erp.model.DbEnum;

/**
 * Converts the text stored in MySQL back into a Java enum constant.
 * It exists so no DAO repeats the same lookup loop.
 */
public final class EnumUtil {

    private EnumUtil() {
    }

    /** Returns the constant whose label matches, or null when the database value is NULL. */
    public static <E extends Enum<E> & DbEnum> E fromLabel(Class<E> type, String label) {
        if (label == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.getLabel().equals(label)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + label);
    }
}
