package com.littlehorizons.erp.model;

/** Academic period. I-IV and FINAL are used by grades, ANNUAL only by payments. */
public enum AcademicPeriod implements DbEnum {
    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    FINAL("Final"),
    ANNUAL("Anual");

    private final String label;

    AcademicPeriod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /** Views print the same text the database stores. */
    @Override
    public String toString() {
        return label;
    }
}
