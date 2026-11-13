package com.littlehorizons.erp.model;

/** Student gender. */
public enum Gender implements DbEnum {
    M("M"),
    F("F"),
    OTHER("Otro");

    private final String label;

    Gender(String label) {
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
